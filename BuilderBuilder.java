package building.Makeshift;

// Changes to this file immediately affect the next build.  Treat it as a build script.

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;

import static building.Makeshift.Bootstrap.addCompilableSource;
import static building.Makeshift.Bootstrap.packageOf;
import static building.Makeshift.Bootstrap.pathOf;
import static building.Makeshift.Bootstrap.pathTester_true;
import static building.Makeshift.Bootstrap.typeName;
import static building.Makeshift.Bootstrap.verify;
import static building.Makeshift.Bootstrap.Unhandled;
import static building.Makeshift.Bootstrap.UserError;


/** A builder of software builders.  In place of the {@linkplain BuilderBuilderDefault default},
  * a project may define its own builder by putting a source file named `BuilderBuilder.java` into its
  * {@linkplain #internalBuildingCode(Path) building code}.  The class definition must be public and must
  * include a public constructor that takes no parameters.  It must inherit from the present interface.
  * It must depend on no code outside of:<ul>
  *
  *     <li>The standard libraries</li>
  *     <li>`{@linkplain Bootstrap             Bootstrap}`</li>
  *     <li>`{@linkplain Builder               Builder}`</li>
  *     <li>                                  `BuilderBuilder` (the present interface)</li>
  *     <li>`{@linkplain BuilderBuilderDefault BuilderBuilderDefault}`</li></ul>
  */// Changing the above?  Sync → stage 1 of `run` in `bin/build`.
public interface BuilderBuilder {


    /** Packages of building code additional to
      * the {@linkplain #internalBuildingCode() internal building code}.  The added code
      * comprises all `.java` files of the {@linkplain Bootstrap#pathOf(String) equivalent directories},
      * exclusive of their subdirectories.  Such code may be intended for the use of other projects, for
      * example, as part of their {@linkplain #externalBuildingCode() <em>external</em> building code}.
      *
      * <p>The default implementation is an empty set.</p>
      *
      *     @see #internalBuildingCode(Path)
      */
    public default Set<String> addedBuildingCode() { return Set.of(); } /* Packages for elements
      because they are codeable by implementers as cross-platform literals, whereas paths are not. */



    /** Compiles the code of the software builder, including any
      * {@linkplain #externalBuildingCode() external building code} on which it depends,
      * and prepares it for use.
      *
      * <p>To get an instance of the builder once built, use {@linkplain #newBuilder() newBuilder}.</p>
      *
      *     @throws IllegalStateException If already a build had started.
      */
    public default void build() throws UserError {
        final String owningProject = projectPackage();
        if( projectsUnderBuild.contains( owningProject )) throw new IllegalStateException();
        projectsUnderBuild.add( owningProject );

      // Build the external building code
      // ────────────────────────────────
        for( final String externalProject: externalBuildingCode() ) { /* Iteration order is unimportant;
              regardless projects will build here in correct order.  Makeshift, for instance,
              will always build before any other project that nominally depends on it. */
            if( projectsUnderBuild.contains( externalProject )) continue;
            forPackage(externalProject).build(); }

      // Compile the project’s own building code
      // ───────────────────────────────────────
        final List<String> sourceNames = new ArrayList<>();
        final Predicate<Path> tester = targetFile().getFileName().toString().equals( "Target.java" ) ?
          pathTester_true : p -> p.getFileName().toString().startsWith("Build");
        addCompilableSource( sourceNames, internalBuildingCode(projectPath()), tester );
        addedBuildingCode().forEach( pkg -> addCompilableSource( sourceNames, pathOf(pkg) ));
        if( sourceNames.size() > 0 ) Bootstrap.compile( owningProject, sourceNames ); }



    /** The proper package of each project, less the {@linkplain #projectPackage() owning project},
      * whose {@linkplain #internalBuildingCode() building code} the software builder may depend on.
      * The default implementation is a singleton set comprising ‘building.Makeshift’.
      *
      *     @see #internalBuildingCode(Path)
      */
    public default Set<String> externalBuildingCode() { return Set.of( "building.Makeshift" ); }



    /** Gives a builder builder for a project, first compiling its code if necessary.
      *
      *     @param projectPackage The proper package of the project.
      */
    public static BuilderBuilder forPackage( final String projectPackage ) throws UserError {
        verify( projectPackage );
        return get( projectPackage, /*projectPath*/pathOf( projectPackage )); }



    /** Gives a builder builder for a project, first compiling its code if necessary.
      *
      *     @param projectPath The proper path of the project.
      */
    public static BuilderBuilder forPath( final Path projectPath ) throws UserError {
        verify( projectPath );
        return get( /*projectPackage*/packageOf(projectPath), projectPath ); }



    /** Gives the proper path of a project’s builder-builder source file.  The given path is either
      * `<i>{@linkplain #internalBuildingCode(Path) internalBuildingCode}</i>/BuilderBuilder.java` if a
      * file exists there, or the path to the {@linkplain BuilderBuilderDefault default implementation}.
      *
      *     @param projectPath The proper path of the project.
      */
    public static Path implementationFile( final Path projectPath ) { // Cf. @ `Builder`.
        verify( projectPath );
        Path p = internalBuildingCode(projectPath).resolve( "BuilderBuilder.java" );
        if( !Files.isRegularFile( p )) p = implementationFileDefault;
        return p; }



    /** The proper path of the source file for the
      * {@linkplain BuilderBuilderDefault default implementation}.
      */
    public static final Path implementationFileDefault =
      Bootstrap.projectPath.resolve( "BuilderBuilderDefault.java" );



    /** Gives the proper path of the directory containing a project’s internal building code.
      * Gives either (a) `<i>projectPath</i>/builder/` if a directory exists there,
      * else (b) `<i>projectPath</i>/`.  A project may store its internal building code
      * in this directory alone, exclusive of subdirectories.
      *
      * <p>Moreover, in the default implementation, if (c) this directory contains a file
      * named `BuildTarget.java`, then it defines the build targets of the project
      * and the building code comprises only those files whose names begin with `Build`.
      * Otherwise the building code comprises all child files of the directory.</p>
      *
      *     @param projectPath The proper path of the project.
      *     @see #addedBuildingCode()
      *     @see #externalBuildingCode()
      *     @see <a href='http://reluk.ca/project/building/Makeshift/example/sub/'      >Example of (a)</a>
      *     @see <a href='http://reluk.ca/project/building/Makeshift/example/top/'      >Example of (b)</a>
      *     @see <a href='http://reluk.ca/project/building/Makeshift/example/mixed_top/'>Example of (c)</a>
      */
    public static Path internalBuildingCode( final Path projectPath ) {
        verify( projectPath );
        Path p = projectPath.resolve( "builder" );
        if( !Files.isDirectory( p )) p = projectPath;
        return p; }



    /** Makes an instance of the software builder, once {@linkplain #build() built}.
      */
    public default Builder newBuilder() {
        try {
            final Class<? extends Builder> cBuilder =
              Class.forName( typeName( Builder.implementationFile( projectPath() )))
              .asSubclass( Builder.class );
            final Class<?> cTarget = Class.forName( typeName( targetFile() )).asSubclass( Enum.class );
            try { // Either (a) the default implementation `BuilderDefault`, or (b) a custom one:
                return cBuilder.getConstructor( Class.class, String.class, Path.class ) // (a)
                  .newInstance( cTarget, projectPackage(), projectPath() ); }
            catch( NoSuchMethodException x ) { return cBuilder.getConstructor().newInstance(); }} // (b)
        catch( ReflectiveOperationException x ) { throw new Unhandled( x ); }}



    /** The proper package of the owning project.
      */
    public String projectPackage();



    /** The proper path of the owning project.
      */
    public Path projectPath();



    /** Projects for which a {@linkplain #build() builder build} was called in the present runtime,
      * each identified by its proper package.
      */
    public static final Set<String> projectsUnderBuild = new HashSet<>();



    /** The proper path of the source file defining the build targets.  The default implementation
      * is a child path of the {@linkplain #internalBuildingCode(Path) internal building code}
      * with a simple name of either `BuildTarget.java` if a file exists there, else `Target.java`.
      */
    public default Path targetFile() {
        final Path iBC = internalBuildingCode( projectPath() );
        Path p = iBC.resolve( "BuildTarget.java" );
        if( !Files.isRegularFile( p )) p = iBC.resolve( "Target.java" );
        return p; }



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    /** Gives a builder builder for a project, first compiling its code if necessary.
      *
      *     @param projectPackage The proper package of the project.
      *     @param projectPath The proper path of the project.
      */
    private static BuilderBuilder get( final String projectPackage, final Path projectPath )
      throws UserError {

      // Compile the code
      // ────────────────
        final Path iFile = implementationFile( projectPath );
        final Path iDirectory = iFile.getParent();
        final String iSimpleTypeName = Bootstrap.simpleTypeName( iFile );
        if( Bootstrap.toCompile( iFile, iSimpleTypeName )) {
            Bootstrap.compile( null/*builder builder*/, List.of( iFile.toString() )); }

      // Construct an instance
      // ─────────────────────
        final String cName = packageOf(iDirectory) + '.' + iSimpleTypeName;
        try {
            final Class<? extends BuilderBuilder> c = Class.forName( cName )
              .asSubclass( BuilderBuilder.class );
            try { // Either (a) the default implementation `BuilderBuilderDefault`, or (b) a custom one:
                return c.getConstructor( String.class, Path.class ) // (a)
                  .newInstance( projectPackage, projectPath ); }
            catch( NoSuchMethodException x ) { return c.getConstructor().newInstance(); }} // (b)
        catch( ReflectiveOperationException x ) { throw new Unhandled( x ); }}}



                                                   // Copyright © 2020-2021  Michael Allan.  Licence MIT.
