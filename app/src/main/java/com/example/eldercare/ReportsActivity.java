package com.example.eldercare;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportsActivity extends BaseActivity {

    private static final String TAG = "ReportsActivity";
    private LineChart chartHeartRate, chartBP;
    private BarChart chartSteps;
    private TabLayout tabPeriod;
    private View scrollContent, loadingIndicator, emptyState;
    private TextView tvTrendAnalysis;
    private MaterialButton btnExportPDF, btnExportCSV, btnDownloadCSV, btnStartLogging;
    private FirebaseFirestore db;
    private String userId;
    private List<VitalsRecord> currentRecords = new ArrayList<>();

    private enum State { LOADING, EMPTY, DATA }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        db = FirebaseFirestore.getInstance();
        String currentUid = FirebaseAuth.getInstance().getUid();

        setupToolbar();
        initViews();
        setupTabs();
        
        if (currentUid != null) {
            setState(State.LOADING);
            db.collection("users").document(currentUid).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String role = doc.getString("role");
                    if ("caregiver".equals(role)) {
                        userId = doc.getString("linkedElderId");
                    } else {
                        userId = currentUid;
                    }

                    if (userId != null) {
                        loadData(7); 
                    } else {
                        setState(State.EMPTY);
                    }
                }
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching user role", e);
                setState(State.EMPTY);
            });
        }
        
        setupBottomNavigation(R.id.nav_reports);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Health Reports");
        }
    }

    private void initViews() {
        chartHeartRate = findViewById(R.id.chartHeartRate);
        chartBP = findViewById(R.id.chartBP);
        chartSteps = findViewById(R.id.chartSteps);
        tabPeriod = findViewById(R.id.tabPeriod);
        tvTrendAnalysis = findViewById(R.id.tvTrendAnalysis);
        
        scrollContent = findViewById(R.id.scrollContent);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        emptyState = findViewById(R.id.emptyState);
        
        btnExportPDF = findViewById(R.id.btnExportPDF);
        btnExportCSV = findViewById(R.id.btnExportCSV); 
        btnDownloadCSV = findViewById(R.id.btnDownloadCSV);
        btnStartLogging = findViewById(R.id.btnStartLogging);

        ChartHelper.applyLineChartStyle(chartHeartRate);
        ChartHelper.applyLineChartStyle(chartBP);
        ChartHelper.applyBarChartStyle(chartSteps);

        btnExportPDF.setOnClickListener(v -> generatePDF());
        if (btnExportCSV != null) {
            btnExportCSV.setOnClickListener(v -> exportToCSV());
        }
        if (btnDownloadCSV != null) {
            btnDownloadCSV.setOnClickListener(v -> downloadCSV());
        }
        if (btnStartLogging != null) {
            btnStartLogging.setOnClickListener(v -> {
                startActivity(new Intent(this, VitalsActivity.class));
            });
        }
    }

    private void setState(State state) {
        scrollContent.setVisibility(state == State.DATA ? View.VISIBLE : View.GONE);
        loadingIndicator.setVisibility(state == State.LOADING ? View.VISIBLE : View.GONE);
        emptyState.setVisibility(state == State.EMPTY ? View.VISIBLE : View.GONE);
    }

    private void setupTabs() {
        tabPeriod.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int days = 7;
                if (tab.getPosition() == 1) days = 30;
                else if (tab.getPosition() == 2) days = 90;
                loadData(days);
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadData(int days) {
        if (userId == null) return;

        setState(State.LOADING);
        
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -days);
        Timestamp threshold = new Timestamp(cal.getTime());

        db.collection("users").document(userId).collection("vitals")
                .whereGreaterThanOrEqualTo("timestamp", threshold)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        setState(State.EMPTY);
                        currentRecords.clear();
                        return;
                    }

                    List<Entry> hrEntries = new ArrayList<>();
                    List<Entry> sysEntries = new ArrayList<>();
                    List<Entry> diaEntries = new ArrayList<>();
                    List<BarEntry> stepEntries = new ArrayList<>();
                    currentRecords.clear();

                    List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();
                    int i = 0;
                    for (int j = docs.size() - 1; j >= 0; j--) {
                        DocumentSnapshot doc = docs.get(j);
                        VitalsRecord record = doc.toObject(VitalsRecord.class);
                        if (record != null) {
                            currentRecords.add(record);
                            hrEntries.add(new Entry(i, record.getHeartRate()));
                            sysEntries.add(new Entry(i, (float) record.getSystolicBP()));
                            diaEntries.add(new Entry(i, (float) record.getDiastolicBP()));
                            stepEntries.add(new BarEntry(i, (float) record.getSteps()));
                            i++;
                        }
                    }

                    if (currentRecords.isEmpty()) {
                        setState(State.EMPTY);
                    } else {
                        updateCharts(hrEntries, sysEntries, diaEntries, stepEntries);
                        calculateTrends();
                        setState(State.DATA);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading report data", e);
                    setState(State.EMPTY);
                });
    }

    private void calculateTrends() {
        if (currentRecords.size() < 2) {
            tvTrendAnalysis.setText("Keep logging to see trend analysis.");
            return;
        }

        double avgHeartRate = 0;
        int totalSteps = 0;
        for (VitalsRecord r : currentRecords) {
            avgHeartRate += r.getHeartRate();
            totalSteps += r.getSteps();
        }
        avgHeartRate /= currentRecords.size();
        
        VitalsRecord latest = currentRecords.get(currentRecords.size() - 1);
        VitalsRecord previous = currentRecords.get(0); // This logic might need refinement for "week over week"
        
        double hrChange = ((latest.getHeartRate() - avgHeartRate) / avgHeartRate) * 100;
        String hrTrend = hrChange > 0 ? "higher" : "lower";
        
        String analysis = String.format(Locale.getDefault(), 
            "• Average Heart Rate: %.0f bpm (%.1f%% %s than period average).\n" +
            "• Total Steps: %d steps.\n" +
            "• Latest BP: %.0f/%.0f mmHg.\n" +
            "• Trend: Your heart rate has been relatively stable.", 
            avgHeartRate, Math.abs(hrChange), hrTrend, totalSteps, 
            latest.getSystolicBP(), latest.getDiastolicBP());
            
        tvTrendAnalysis.setText(analysis);
    }

    private void updateCharts(List<Entry> hr, List<Entry> sys, List<Entry> dia, List<BarEntry> steps) {
        LineDataSet hrSet = ChartHelper.createVitalDataSet(hr, "Heart Rate", Color.RED);
        chartHeartRate.setData(new LineData(hrSet));
        chartHeartRate.invalidate();

        LineDataSet sysSet = ChartHelper.createVitalDataSet(sys, "Systolic", Color.BLUE);
        LineDataSet diaSet = ChartHelper.createVitalDataSet(dia, "Diastolic", Color.CYAN);
        chartBP.setData(new LineData(sysSet, diaSet));
        chartBP.invalidate();

        BarDataSet stepSet = new BarDataSet(steps, "Steps");
        stepSet.setColor(Color.GREEN);
        chartSteps.setData(new BarData(stepSet));
        chartSteps.invalidate();
    }

    private void generatePDF() {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        
        int y = 50;
        paint.setTextSize(24);
        paint.setFakeBoldText(true);
        canvas.drawText("ElderCare Health Report", 50, y, paint);
        
        y += 40;
        paint.setTextSize(14);
        paint.setFakeBoldText(false);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        canvas.drawText("Generated on: " + sdf.format(new Date()), 50, y, paint);
        
        y += 30;
        canvas.drawText("Health Insights:", 50, y, paint);
        y += 20;
        String[] lines = tvTrendAnalysis.getText().toString().split("\n");
        for (String line : lines) {
            canvas.drawText(line, 70, y, paint);
            y += 20;
        }

        y += 30;
        canvas.drawText("Recent Vital Records:", 50, y, paint);
        y += 20;
        canvas.drawText("Date | HR | BP | Steps", 50, y, paint);
        y += 5;
        canvas.drawLine(50, y, 500, y, paint);
        y += 20;

        for (int i = currentRecords.size() - 1; i >= 0 && y < 800; i--) {
            VitalsRecord r = currentRecords.get(i);
            String dateStr = r.getTimestamp() != null ? new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(r.getTimestamp().toDate()) : "N/A";
            String row = String.format(Locale.getDefault(), "%s | %d | %.0f/%.0f | %d", 
                dateStr, r.getHeartRate(), r.getSystolicBP(), r.getDiastolicBP(), r.getSteps());
            canvas.drawText(row, 50, y, paint);
            y += 20;
        }

        document.finishPage(page);

        File folder = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Reports");
        if (!folder.exists()) folder.mkdirs();
        File file = new File(folder, "ElderCare_Report_" + System.currentTimeMillis() + ".pdf");

        try {
            document.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "PDF Report generated!", Toast.LENGTH_LONG).show();
            
            Uri uri = FileProvider.getUriForFile(this, "com.example.eldercare.fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Open Report"));
            
        } catch (IOException e) {
            Log.e(TAG, "PDF Error", e);
            Toast.makeText(this, "PDF Generation failed", Toast.LENGTH_SHORT).show();
        } finally {
            document.close();
        }
    }

    private void downloadCSV() {
        if (currentRecords.isEmpty()) {
            Toast.makeText(this, "No data to download", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder csvData = new StringBuilder();
        csvData.append("Date,Heart Rate,Systolic BP,Diastolic BP,Steps\n");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        for (VitalsRecord record : currentRecords) {
            String date = record.getTimestamp() != null ? sdf.format(record.getTimestamp().toDate()) : "N/A";
            csvData.append(date).append(",")
                    .append(record.getHeartRate()).append(",")
                    .append(record.getSystolicBP()).append(",")
                    .append(record.getDiastolicBP()).append(",")
                    .append(record.getSteps()).append("\n");
        }

        try {
            File downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloadsFolder, "HealthReport_" + System.currentTimeMillis() + ".csv");
            FileOutputStream out = new FileOutputStream(file);
            out.write(csvData.toString().getBytes());
            out.close();

            Toast.makeText(this, "Downloaded to: " + file.getName(), Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            Log.e(TAG, "CSV Download failed", e);
            Toast.makeText(this, "Download failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void exportToCSV() {
        if (currentRecords.isEmpty()) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder csvData = new StringBuilder();
        csvData.append("Date,Heart Rate,Systolic BP,Diastolic BP,Steps\n");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        for (VitalsRecord record : currentRecords) {
            String date = record.getTimestamp() != null ? sdf.format(record.getTimestamp().toDate()) : "N/A";
            csvData.append(date).append(",")
                    .append(record.getHeartRate()).append(",")
                    .append(record.getSystolicBP()).append(",")
                    .append(record.getDiastolicBP()).append(",")
                    .append(record.getSteps()).append("\n");
        }

        try {
            File folder = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Reports");
            if (!folder.exists()) folder.mkdirs();

            File file = new File(folder, "HealthReport_" + System.currentTimeMillis() + ".csv");
            FileOutputStream out = new FileOutputStream(file);
            out.write(csvData.toString().getBytes());
            out.close();

            Uri uri = FileProvider.getUriForFile(this, "com.example.eldercare.fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share Report"));

        } catch (IOException e) {
            Log.e(TAG, "CSV Export failed", e);
        }
    }
}
