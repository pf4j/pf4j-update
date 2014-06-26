PF4J - Update
=====================
The goal of this project is to supply an update mechanism for [PF4J](https://github.com/decebals/pf4j).

Components
-------------------
- **UpdateManager** allows to interrogates the plugins (local, remote) repositories for updates, available (new) plugins. Also this class allow you to install available plugins, update or uninstall existing plugins.
- **UpdateRepository** defines a local/remote repository
- **PluginInfo** defines the plugin information for each repository's plugin
- **PluginRelease** defines a plugin release

How to use
-------------------

_UpdateManager_ can work with multiple repositories (local and remote). All repositories are defined in a `repositories.json` file.

For a remote repository the repository.json looks like:
```json
[
  {
    "id": "localhost",
    "url": "http://localhost:8081/"
  }
]
```

For a local repository the repository.json looks like:
```json
[
  {
    "id": "localhost",
    "url": "file:/home/decebal_suiu/work/pf4j-update/downloads/"
  }
]
```

In the root of the project you have a repositories.json file used by the test applications.

TODO: provide more information about the format of repositories.json

Each repository has metadata stored in a `plugins.json` file.
```json
[
  {
    "id": "welcome-plugin",
    "description": "Welcome plugin",
    "releases": [
      {
        "version": "0.9.0",
        "date": "Jun 25, 2014 9:58:35 PM",
        "url": "pf4j-demo-plugin1/0.9.0/pf4j-demo-plugin1-0.9.0-SNAPSHOT.zip"
      }
    ]
  },
  {
    "id": "hello-plugin",
    "description": "Hello plugin",
    "releases": [
      {
        "version": "0.9.0",
        "date": "Jun 25, 2014 9:58:35 PM",
        "url": "pf4j-demo-plugin2/0.9.0/pf4j-demo-plugin2-0.9.0-SNAPSHOT.zip"
      }
    ]
  }
]
```

TODO: provide more information about the format of plugins.json

In the downloads folder you have a repository (plugins.json and artifacts - plugins archives) used by the test applications.

For more information please see the test sources (UpdateTest, ...).

License
--------------
Copyright 2014 Decebal Suiu

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this work except in compliance with
the License. You may obtain a copy of the License in the LICENSE file, or at:

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
specific language governing permissions and limitations under the License.
