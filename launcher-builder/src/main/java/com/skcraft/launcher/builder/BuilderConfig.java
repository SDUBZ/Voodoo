// Generated by delombok at Sat Jul 14 01:46:55 CEST 2018
/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */
package com.skcraft.launcher.builder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import com.skcraft.launcher.model.modpack.LaunchModifier;
import com.skcraft.launcher.model.modpack.Manifest;
import java.util.List;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.emptyToNull;

public class BuilderConfig {
    private String name;
    private String title;
    private String gameVersion;
    @JsonProperty("launch")
    private LaunchModifier launchModifier = new LaunchModifier();
    private List<FeaturePattern> features = Lists.newArrayList();
    private FnPatternList userFiles = new FnPatternList();

    public void setLaunchModifier(LaunchModifier launchModifier) {
        this.launchModifier = launchModifier != null ? launchModifier : new LaunchModifier();
    }

    public void setFeatures(List<FeaturePattern> features) {
        this.features = features != null ? features : Lists.<FeaturePattern>newArrayList();
    }

    public void setUserFiles(FnPatternList userFiles) {
        this.userFiles = userFiles != null ? userFiles : new FnPatternList();
    }

    public void update(Manifest manifest) {
        manifest.updateName(getName());
        manifest.updateTitle(getTitle());
        manifest.updateGameVersion(getGameVersion());
        manifest.setLaunchModifier(getLaunchModifier());
    }

    public void registerProperties(PropertiesApplicator applicator) {
        if (features != null) {
            for (FeaturePattern feature : features) {
                checkNotNull(emptyToNull(feature.getFeature().getName()), "Empty feature name found");
                applicator.register(feature);
            }
        }
        applicator.setUserFiles(userFiles);
    }

    @java.lang.SuppressWarnings("all")
    public BuilderConfig() {
    }

    @java.lang.SuppressWarnings("all")
    public String getName() {
        return this.name;
    }

    @java.lang.SuppressWarnings("all")
    public String getTitle() {
        return this.title;
    }

    @java.lang.SuppressWarnings("all")
    public String getGameVersion() {
        return this.gameVersion;
    }

    @java.lang.SuppressWarnings("all")
    public LaunchModifier getLaunchModifier() {
        return this.launchModifier;
    }

    @java.lang.SuppressWarnings("all")
    public List<FeaturePattern> getFeatures() {
        return this.features;
    }

    @java.lang.SuppressWarnings("all")
    public FnPatternList getUserFiles() {
        return this.userFiles;
    }

    @java.lang.SuppressWarnings("all")
    public void setName(final String name) {
        this.name = name;
    }

    @java.lang.SuppressWarnings("all")
    public void setTitle(final String title) {
        this.title = title;
    }

    @java.lang.SuppressWarnings("all")
    public void setGameVersion(final String gameVersion) {
        this.gameVersion = gameVersion;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof BuilderConfig)) return false;
        final BuilderConfig other = (BuilderConfig) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$name = this.getName();
        final java.lang.Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final java.lang.Object this$title = this.getTitle();
        final java.lang.Object other$title = other.getTitle();
        if (this$title == null ? other$title != null : !this$title.equals(other$title)) return false;
        final java.lang.Object this$gameVersion = this.getGameVersion();
        final java.lang.Object other$gameVersion = other.getGameVersion();
        if (this$gameVersion == null ? other$gameVersion != null : !this$gameVersion.equals(other$gameVersion)) return false;
        final java.lang.Object this$launchModifier = this.getLaunchModifier();
        final java.lang.Object other$launchModifier = other.getLaunchModifier();
        if (this$launchModifier == null ? other$launchModifier != null : !this$launchModifier.equals(other$launchModifier)) return false;
        final java.lang.Object this$features = this.getFeatures();
        final java.lang.Object other$features = other.getFeatures();
        if (this$features == null ? other$features != null : !this$features.equals(other$features)) return false;
        final java.lang.Object this$userFiles = this.getUserFiles();
        final java.lang.Object other$userFiles = other.getUserFiles();
        if (this$userFiles == null ? other$userFiles != null : !this$userFiles.equals(other$userFiles)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof BuilderConfig;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final java.lang.Object $title = this.getTitle();
        result = result * PRIME + ($title == null ? 43 : $title.hashCode());
        final java.lang.Object $gameVersion = this.getGameVersion();
        result = result * PRIME + ($gameVersion == null ? 43 : $gameVersion.hashCode());
        final java.lang.Object $launchModifier = this.getLaunchModifier();
        result = result * PRIME + ($launchModifier == null ? 43 : $launchModifier.hashCode());
        final java.lang.Object $features = this.getFeatures();
        result = result * PRIME + ($features == null ? 43 : $features.hashCode());
        final java.lang.Object $userFiles = this.getUserFiles();
        result = result * PRIME + ($userFiles == null ? 43 : $userFiles.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "BuilderConfig(name=" + this.getName() + ", title=" + this.getTitle() + ", gameVersion=" + this.getGameVersion() + ", launchModifier=" + this.getLaunchModifier() + ", features=" + this.getFeatures() + ", userFiles=" + this.getUserFiles() + ")";
    }
}
