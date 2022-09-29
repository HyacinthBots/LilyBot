# LilyBot X.X.X

Throughout this file, examples are placed in code blocks.

X.X.X above should be replaced with the version number where
* breaking versions are something that requires a significant change in the environment to run Lily
* major versions have a fair few features, changes, and fixes
* minor versions are simply a patch

Next should be a description of the features of the update.
```
This long-awaited update rewrites Lily in Rust. This will be the last update, as the bot is now perfect.
```

It's important that this end with the following statement.
```
You can find the full changelog below.
```

The changelog should then be split into three categories: New, Change, & Fix.
These are fairly self-explanatory buckets, with new features going in the first, 
changes to existing functionality going in the second, and restorations of intended functionality in the third.

```
New:
* very memory safe
* lots of crabs

Change:
* use rust instead of Kotlin
* auto-ban anyone who says the word Kotlin

Fix:
* literally every bug, Rust is perfect
* we've even fixed bugs Discord hasn't thought of
```

The changelog should then end with the following
```
You can find a list of all the commits in this update [here](https://github.com/hyacinthbots/LilyBot/compare/vP.P.P...vX.X.X)
```
where P.P.P is replaced with the previous version number and X.X.X is the new version number.

The changelog should be copied and pasted into the GitHub release, excepting the header.
This changelog can then be trimmed or adjusted if necessary for publication on Discord.
If need be, a more concise version can be sent out via the Lily's announcement system.
