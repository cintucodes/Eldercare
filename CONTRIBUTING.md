# Contributing to ElderCare

First off, thank you for considering contributing to ElderCare! It's people like you that make ElderCare such a great tool for helping elderly care.

## Code of Conduct

This project and everyone participating in it is governed by respect, kindness, and professionalism. By participating, you are expected to uphold this code.

## How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check the existing issues to avoid duplicates. When you create a bug report, include as many details as possible:

- **Use a clear and descriptive title**
- **Describe the exact steps to reproduce the problem**
- **Provide specific examples** (screenshots, code snippets)
- **Describe the behavior you observed and what you expected**
- **Include device/Android version details**

### Suggesting Enhancements

Enhancement suggestions are tracked as GitHub issues. When creating an enhancement suggestion:

- **Use a clear and descriptive title**
- **Provide a detailed description of the suggested enhancement**
- **Explain why this enhancement would be useful**
- **List any similar features in other apps** (if applicable)

### Pull Requests

1. **Fork the repo** and create your branch from `main`
2. **Follow the existing code style**:
   - Use meaningful variable names
   - Add comments for complex logic
   - Follow Java/Kotlin conventions
3. **Test your changes thoroughly**:
   - Test on multiple Android versions if possible
   - Ensure no existing features are broken
4. **Update documentation** if needed
5. **Write a clear commit message**:
   ```
   feat: Add medication interaction warnings
   fix: Resolve notification sound issue on Android 13
   docs: Update README with new features
   ```

### Code Style Guidelines

**Java:**
- Use camelCase for variables and methods
- Use PascalCase for class names
- Add JavaDoc comments for public methods
- Keep methods focused and under 50 lines when possible

**Kotlin:**
- Follow Kotlin coding conventions
- Use data classes for models
- Prefer immutability

**XML:**
- Use meaningful IDs (e.g., `btnSaveProfile`, not `button1`)
- Follow Material Design guidelines
- Keep layouts modular with `<include>` tags

### Commit Message Format

```
<type>: <subject>

<body>

<footer>
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, no logic change)
- `refactor`: Code refactoring
- `test`: Adding tests
- `chore`: Maintenance tasks

## Development Setup

1. Clone your fork
2. Set up Firebase (see README.md)
3. Open in Android Studio
4. Create a feature branch: `git checkout -b feature/my-feature`
5. Make your changes
6. Test thoroughly
7. Commit: `git commit -m "feat: Add my feature"`
8. Push: `git push origin feature/my-feature`
9. Open a Pull Request

## Testing Checklist

Before submitting a PR, ensure:

- [ ] Code compiles without errors
- [ ] No new warnings introduced
- [ ] Tested on Android 7.0+ (API 24+)
- [ ] UI looks good on different screen sizes
- [ ] No crashes or ANRs
- [ ] Firebase operations work correctly
- [ ] Notifications display properly
- [ ] Permissions are handled correctly

## Priority Areas for Contribution

We especially welcome contributions in these areas:

1. **Accessibility improvements** (TalkBack, large text support)
2. **Internationalization** (translations)
3. **UI/UX enhancements**
4. **Performance optimizations**
5. **Test coverage**
6. **Documentation improvements**

## Questions?

Feel free to open an issue with the `question` label or reach out to the maintainers.

## Recognition

Contributors will be recognized in the README.md file and release notes.

Thank you for contributing! 🎉
