# GitHub password searcher
Team: Mykola Melnychuk, Oles Dobosevych
Search for tokens and secret via GitHub API.
## Setup
Replace GITHUB_TOKEN within Config class with your GitHub token. If you need to setup Github token follow next steps:
1. On any GitHub page, click your profile icon and then click Settings.
2. On the sidebar, click Personal access tokens.
3. Click Generate new token.
4. Add a token description and click Generate token.
5. Copy the token to a secure location or password management app.

Application can be tested using chrome extension on [webstore](https://chrome.google.com/webstore/detail/websocket-test-client/fgponpodhbmadfljofbimhhlengambbn)

To get a stream of new exposed keys and highlights of projects which exposed it's first key:

``ws://localhost:8080/stream?services=Facebook``

"services" is optional for filtering service keys. For multiple entries repeat the parameter

To get a regular update on keys leakage statistics per language use

``ws://localhost:8080/stats?tick=1&unit=s``

"tick" - interval of getting statistics
"unit" - it's time unit `s` `ms` `min`