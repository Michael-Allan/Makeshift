package building;

// Changes to this file immediately affect the next runtime.  Treat it as a script.

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;

import static java.io.File.separatorChar;


/** A miscellany of resources for building software builders, residual odds and ends that properly fit
  * nowhere else during the earliest build stages.
  */
public final class Bootstrap {


    private Bootstrap() {}



    /** The proper path of the building project.
      */
    public static final Path buildingProjectPath = Path.of( "building" );



    /** The single instance of `Bootstrap`.
      */
    public static final Bootstrap i = new Bootstrap();



    /** Converts `path` to an equivalent Java package name.
      *
      *     @throws IllegalArgumentException If `path` is absolute.
      */
    public static String packageOf( final Path path ) {
        if( path.isAbsolute() ) throw new IllegalArgumentException();
        return path.toString().replace( separatorChar, '.' ); }



    /** Converts `JavaPackage` to an equivalent relative path.
      */
    public static Path pathOf( final String JavaPackage ) {
        return FileSystems.getDefault().getPath( pathStringOf( JavaPackage )); }



    /** Converts `JavaPackage` to an equivalent relative path.
      */
    public static String pathStringOf( final String JavaPackage ) {
        return JavaPackage.replace( '.', separatorChar ); }



    /** A path tester that answers only `true`.
      */
    public static final Predicate<Path> pathTester_true = _p -> { return true; };



    /** Prints and flushes through standard output the beginning of a message of incremental
      * build progress.  Be sure to print the remainder and terminate it with a newline character.
      *
      *     @param projectPackage The proper package of the project whose software is being built,
      *       or null if the software builder itself is being built.
      *     @param type A short name to identify the type of progress.
      */
    public void printProgressLeader( final String projectPackage, final String type ) {
        final String project = projectPackage == null? "(bootstrap)": projectPackage;
        if( !projectsShowingProgress.contains( project )) {
            System.out.println( project );
            projectsShowingProgress.add( project ); }
        System.out.print( "    " );
        System.out.print( type );
        System.out.print( ' ' );
        System.out.flush(); }



    /** Tests the validity of a `targetClass` given as the class of build targets for a project.
      *
      *     @throws IllegalArgumentException
      */
    public <T extends Enum<T>> void verify( final Class<T> targetClass ) {
        Enum.valueOf( targetClass, "builder" ); }



    /** Tests for consistency between parameters given for a project.
      * Where applicable, individually test each parameter before calling this method.
      *
      *     @param targetClass The class of the project’s build targets.
      *     @param projectPackage The proper package of the project.
      *     @throws IllegalArgumentException
      */
    public void verify( final Class<?> targetClass, final String projectPackage ) {
        final String iBC = targetClass.getPackageName(); /* Of `BuilderBuilder.internalBuildingCode`,
          that is, according to whose API documentation one of the following tests must pass. */
        if( iBC.equals( projectPackage )) return;
        if( iBC.length() == projectPackage.length() + ".builder".length()
          && iBC.startsWith( projectPackage )
          && iBC.endsWith( ".builder" )) return;
        throw new IllegalArgumentException(
          "Inconsistency between `projectPackage` and `targetClass` package" ); }



    /** Tests the validity of a `projectPath` given as the proper path of a project.
      *
      *     @throws IllegalArgumentException
      */
    public void verify( final Path projectPath ) {
        if( projectPath.isAbsolute() ) throw new IllegalArgumentException( "Absolute `projectPath`" );
        if( projectPath.getFileName().toString().equals( "builder" )) {
          throw new IllegalArgumentException( "Project path ends with `builder`: " + projectPath ); }}
          // Simpler than trying to fathom the repercussions of allowing it, given that subdirectory
          // `builder/` is reserved for a project’s building code.



    /** Tests the validity of a `projectPackage` given as the proper package of a project.
      *
      *     @throws IllegalArgumentException
      */
    public void verify( final String projectPackage ) {
        if( projectPackage.equals("builder") || projectPackage.endsWith( ".builder" )) {
          throw new IllegalArgumentException( "Project package ends with `builder`: "
            + projectPackage ); }} // Simpler than allowing it, as explained for `#verify(Path)`.



    /** Tests for consistency between parameters given for a project.
      * Where applicable, individually test each parameter before calling this method.
      *
      *     @param projectPackage The proper package of the project.
      *     @param projectPath The proper path of the project.
      *     @throws IllegalArgumentException
      */
    public void verify( final String projectPackage, final Path projectPath ) {
        if( !pathStringOf(projectPackage).equals( projectPath.toString() )) {
            throw new IllegalArgumentException( "Inequivalent `projectPackage` and `projectPath`" ); }}



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    private final Set<String> projectsShowingProgress = new HashSet<>(); } /* Generally a maximum
      of two members, unless one project has customized its build to entail the build of another. */



                                                        // Copyright © 2020  Michael Allan.  Licence MIT.
