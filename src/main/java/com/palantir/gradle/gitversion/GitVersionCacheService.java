/*
 * (c) Copyright 2023 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.gradle.gitversion;

import java.io.File;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

public abstract class GitVersionCacheService implements BuildService<GitVersionCacheService.Params> {

    private final Timer timer = new Timer();

    interface Params extends BuildServiceParameters {

        Property<Project> getProject();
    }

    private final String gitVersion;
    private final VersionDetails versionDetails;

    public GitVersionCacheService() {
        Object args = new Object();
        final Project project = getParameters().getProject().get();
        final File git = gitRepo(project);
        versionDetails =
                TimingVersionDetails.wrap(timer, new VersionDetailsImpl(git, GitVersionArgs.fromGroovyClosure(args)));
        gitVersion = versionDetails.getVersion();
    }

    public final String getGitVersion() {
        return gitVersion;
    }

    public final VersionDetails getVersionDetails() {
        return versionDetails;
    }

    public final Timer timer() {
        return timer;
    }

    private File gitRepo(Project project) {
        File gitDir = getRootGitDir(project.getProjectDir());
        return gitDir;
    }

    private static File getRootGitDir(File currentRoot) {
        File gitDir = scanForRootGitDir(currentRoot);
        if (!gitDir.exists()) {
            throw new IllegalArgumentException("Cannot find '.git' directory");
        }
        return gitDir;
    }

    private static File scanForRootGitDir(File currentRoot) {
        File gitDir = new File(currentRoot, ".git");

        if (gitDir.exists()) {
            return gitDir;
        }

        // stop at the root directory, return non-existing File object;
        if (currentRoot.getParentFile() == null) {
            return gitDir;
        }

        // look in parent directory;
        return scanForRootGitDir(currentRoot.getParentFile());
    }
}
