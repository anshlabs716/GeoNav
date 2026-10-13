# Contributing to GeoNav

Thanks for your interest in improving GeoNav. This document explains how to
propose changes.

## Ways to contribute

- Reporting bugs (use the issue templates)
- Suggesting features
- Improving documentation
- Submitting code changes

## Before you start

- For anything larger than a small fix, open an issue first so the approach
  can be discussed.
- Search existing issues and pull requests to avoid duplicates.

## Development setup

See [docs/development.md](docs/development.md) for the full environment setup.

```bash
git clone https://github.com/anshlabs716/GeoNav.git
cd GeoNav
./gradlew assembleDebug
```

## Branching

- `main` is the stable branch.
- Create a topic branch from `main`, e.g. `fix/short-description` or
  `feat/short-description`.
- Keep one logical change per pull request.

## Commit messages

Use short, imperative subjects, for example:

```text
fix: correct permissions request on first launch
docs: clarify export format
```

## Pull requests

- Fill in the pull request template.
- Reference related issues (`Closes #12`).
- Make sure the build and tests pass (`./gradlew test`).
- Update documentation and `CHANGELOG.md` when behavior changes.

## Code style

Match the existing style of the project. Do not reformat unrelated code in the
same pull request. Keep changes focused and reviewable.

## License

By contributing you agree that your contributions are licensed under the same
terms as this project. See the repository `LICENSE` file.
