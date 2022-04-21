## Issues

When opening issues, please make sure that you include as much information as applicable.

* If your issue is an error, include a
  [stacktrace](https://stackoverflow.com/questions/3988788/what-is-a-stack-trace-and-how-can-i-use-it-to-debug-my-application-errors)
  of what went wrong. This can be attached as a file, or uploaded to a [GitHub Gist](https://gist.github.com/).
* If your issue is a bug or otherwise unexpected behavior, explain what happened and what you expected to happen.
* If you're having trouble setting the bot up, we recommend joining [our Discord](https://discord.gg/hy2329fcTZ)
  as we will be able to provide more dynamic support there.

## Pull Requests

We love to see when people contribute to the project! Our [Issues Page](https://github.com/IrisShaders/LilyBot/issues)
often has a list of features that need to be implemented. However, you'll need to read the following guidelines:

### Contributor License Agreement (CLA)

By submitting changes to the repository, you are hereby agreeing that:

* Your contributions will be licensed irrevocably under the
  [GNU General Public License v3.0](https://choosealicense.com/licenses/gpl-3.0/).
* Your contributions are of your own work and free of legal restrictions (such as patents and copyrights)
  or other issues which would pose issues for inclusion or distribution under the above license.

If you do not agree with these terms, please do not submit contributions to this repository. If you have any questions
about these terms, feel free to join [our Discord](https://discord.gg/hy2329fcTZ)
and ask any questions that you may have.

## Pull Request Policy

This PR policy has been adapted from the
[Quilt Mappings](https://github.com/QuiltMC/quilt-mappings/blob/22w14a/CONTRIBUTING.md#guide-pull-requests) PR policy

1. ### Look at the code style
   Before you begin work on adding code to LilyBot, please spend some time looking through the rest of the code in
   LilyBot, and ensure in your Pull Request you follow the style as closely as you can.

   If you have any questions, do not hesitate to ask in [our Discord](https://discord.gg/hy2329fcTZ).

2. ### Open your PR and wait for reviews
   Once you have forked LilyBot and opened a pull request, you will need to wait for reviews from other people. When you
   get reviews, try to thoughtfully address any concerns other people have. If you get confused, be sure to ask
   questions!

3. ### Entering a Final Comment Period
   Once your PR has no "changes requested" reviews, accumulated the minimum number of reviews for its
   [category](#pull-request-categories), and nobody has an outstanding review request, it is eligible to enter a Final
   Comment Period (FCP). An FCP is a final call for any reviewers to look at your PR before it is merged. The minimum
   length of your PR's FCP is determined by its [category](#available-pr-categories), but if further changes are
   requested, the FCP may be extended, or if the concerns are significant, the FCP cancelled until the concerns are
   addressed and resolved.

4. ### Request a merge!
   Once the minimum time on the Final Comment Period has passed, and you have resolved any concerns reviewers have
   raised during that time, leave a comment on your PR requesting for it to be merged, unless it has already been merged
   before you leave the comment. A Development team member will then take a final look over your PR, and if everything
   looks good, merge it.

## Pull Request Categories

The categories ensure that important, but small PRs - like bugfixes - are merged quickly, while larger changes - like a
new feature, or refactor - are thoroughly reviewed before they are merged.

### Available PR Categories

Everything within this section is the definitions for the actual PR policy followed by the Development team.

The listed types are based on the labels that can be assigned to PR's in the LilyBot repository

### `PRT: bug-fix`

**Description**: Used for pull requests that explicitly fix a bug in the code

**Final Comment Period**: 1 day

**Special Cases**:

- If the PR is marked as urgent, the PR may be merged once a review has been submitted from NoComment1105 or tempest15

### `PRT: documentation`

**Description**: Used for pull requests that focus mainly on adding or changing documentation

**Final Comment Period**: 2 days

### `PRT: refactor`

**Description**: Used for pull requests that focus on changing or rewriting something

**Final Comment Period**: 3 days

### `PRT: new-feature`

**Description**: Used for pull requests that focus primarily on adding brand-new features to LilyBot

**Final Comment Period**: 4 days

### `PRT: release-staging`

**Description**: Used for pull requests that merge develop into main

**Final Comment Period**: 5 days

### Other

Trivial fixes that do not require review (e.g. typos) are exempt from this policy. Development team members should
double-check with other members of the team on Discord before pushing a commit or merging a PR without going through
this process.

PRs that do not fit under any of these categories but are not "trivial fixes" are merged at the consensus of the
Development team, using whatever criteria they determine to be appropriate.
