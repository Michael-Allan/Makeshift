package Makeshift.builder;

// Changes to this file immediately affect the next build.  Treat it as a build script.

import java.nio.file.Path;
import java.util.Set;


/** The builder builder proper to Makeshift.
  */
public final class BuilderBuilder extends Makeshift.BuilderBuilderDefault {


    public BuilderBuilder() {
        super( "Makeshift", Makeshift.Bootstrap.projectPath ); }


    public @Override Set<String> addedBuildingCode() {
        return Set.of( "Makeshift", "Makeshift.template" ); }


    public @Override Set<String> externalBuildingCode() { return Set.of(); }}



                                                        // Copyright © 2020  Michael Allan.  Licence MIT.
