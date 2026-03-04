---                                                                                                                                                                          
name: mark-test-for-review
description: Mark new , deleted or modified tests for review
disable-model-invocation: true
allowed-tools: "WebFetch(domain:github.com)", "WebFetch(domain:raw.githubusercontent.com)", "Bash(cat:*)", "Bash(grep:*)", "Bash(curl:*)", "Bash(git:*)","Bash(echo:*)","Bash(gh api:*)","Bash(gh repo:*)","Bash(gh label:*)","Bash(gh issue:*)"
context: fork
---

Check the most recent commit, inspect code and resources in src/test , look for changes, additions and deletions in test code (src/test/java) 
and/or test resources (src/test/resources) that have been made by claude code. 

If there are changes, additions or deletions of test resources, associate them with the tests that access the respective resource file in code; use grep to search src/test/java for the resource filename to find which test classes reference it.

If any changes, additions or deletions are detected, create a new issue (called `root issue`) as follows: 

- name the issue `review-AI-generated-tests-<counter>` 
- the `<counter>` is initially 1, if issues using the same naming pattern exist, use the next available counter
- label this root issue as `review-AI-generated`
- for each test class that has been changed, added or removed create a sub-issue (called `classlevel issue`) of the root issue
- name this issue `review-AI-generated-tests-<counter>-<classname>`
- label each of those classlevel issues as `review-AI-generated`
- for each test (as in: test method) or fixture that has been added, modified or removed create a sub-issue (called `methodlevel issue`) of the class-level issue
- name this issue `review-AI-generated-tests-<counter>-<classname>-<methodname>`
- label each of those methodlevel issues as `review-AI-generated`
- use the GitHub sub-issues API to create subissues

If no changes are detected, report that and stop.

For each method level issue, include the following: 

- the actual test code in a code block
- a link to the test code using the correct commit (unless the test has been removed)
- the content of any resources referenced in tests
- a link to the resources referenced in tests using the correct commit
- if resources and tests have changed, also include diffs, and links to the respective resources in the parent commits


Prompt for the person to assign this to, list project members as choices.