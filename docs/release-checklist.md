# LilyBot Release Checklist

This is a list of everything that needs to happen to release a new version of LilyBot.

* Merge all outstanding pull requests planned to be included in the release
* Create changelog in `docs/changelogs`
* Bump version
* Draft GitHub release containing the full changelog
* Draft Discord announcement containing notes and a trimmed changelog
* Create a release pull request
* Merge the release PR without squashing
* Wait for build and deploy
* Publish release on GitHub
* Announce on Discord
* Clean up issues, specifically remove the `fixed in next release` tag
