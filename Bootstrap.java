package building.Makeshift;

// Changes to this file immediately affect the next build.  Treat it as a build script.

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.io.File.separatorChar;
import static java.nio.file.Files.getLastModifiedTime;


/** A medley of early-use resources for the present project, residual odds and ends that properly fit
  * nowhere else during the earliest build stage.  This class is for the use of custom builder builders
  * and their {@linkplain BuilderBuilder allowed dependencies}; other code may access the same resources
  * through the `{@linkplain Project Project}` class.
  */
public class Bootstrap {


    Bootstrap() {}



    /** Appends to `names` the proper path of each `.java` file of `directory` that needs to be compiled
      * or recompiled.  Does not descend into subdirectories.
      *
      *     @see #compile(List)
      */
    public static void addCompilableSource( final List<String> names, final Path directory ) {
        addCompilableSource( names, directory, pathTester_true ); }



    /** Appends to `names` the proper path of each `.java` file of `directory` that a) tests true
      * with `tester` and b) needs to be compiled or recompiled.  Does not descend into subdirectories.
      *
      *     @see #compile(List)
      */
    public static void addCompilableSource( final List<String> names, final Path directory,
          final Predicate<Path> tester ) {
        try( final Stream<Path> pp = Files.list( directory )) {
            for( final Path p: (Iterable<Path>)pp::iterator ) {
                if( Files.isDirectory( p )) continue;
                final String name = p.toString();
                if( !name.endsWith( ".java" )) continue;
                if( !tester.test( p )) continue;
                if( toCompile( p, simpleTypeName(p) )) names.add( name ); }}
        catch( IOException x ) { throw new Unhandled( x ); }}



    /** Compiles Java source code to class files.
      *
      *     @param projectPackage The proper package of the project whose source code is being compiled,
      *       or null if the builder builder is being compiled.
      *     @param sourceNames The proper path of each source file to compile.
      */
    public static void compile( final String projectPackage, final List<String> sourceNames )
          throws UserError {
        compile( projectPackage, sourceNames, List.of() ); }



    /** Compiles Java source code to class files.
      *
      *     @param projectPackage The proper package of the project whose source code is being compiled,
      *       or null if the builder builder is being compiled.
      *     @param sourceNames The proper path of each source file to compile.
      *     @param additionalArguments Additional arguments for `javac`.  These will be inserted
      *       before the given source names.
      *     @see <a href='https://docs.oracle.com/en/java/javase/17/docs/specs/man/javac.html#synopsis'>
      *       Synopsis of `javac`</a>
      */
    public static void compile( final String projectPackage, final List<String> sourceNames,
          final List<String> additionalArguments ) throws UserError {
        // Changing?  Sync → `run` @ `bin/build`.
        printProgressLeader( projectPackage, "javac" );
        final List<String> compilerArguments = new ArrayList<>();
        compilerArguments.add( System.getProperty("java.home") + "/bin/javac" );
          // The Java installation at `java.home` is known to include `javac` because also
          // it is a JDK installation, as assured by the `JDK_HOME` atop `bin/build`.
        compilerArguments.add( "@building/Makeshift/java_javac_arguments" );
        compilerArguments.add( "@building/Makeshift/javac_arguments" );
        compilerArguments.addAll( additionalArguments );
        compilerArguments.addAll( sourceNames );
        final ProcessBuilder pB = new ProcessBuilder( compilerArguments );
        pB.redirectErrorStream( true );
        final StringBuilder capture = new StringBuilder(); // E.g. of compiler warnings or errors.
        try {
            final Process p = pB.start();
            appendAll( p, capture );
            final int exitValue =  p.waitFor();
            if( exitValue == 1 ) throw new UserError( "Stopped on `javac` error" );
              // Already `javac` has told the details.
            else if( exitValue != 0 ) throw new Unhandled( "Exit value of " + exitValue
              + " from process: " + pB.command() ); }
        catch( final InterruptedException x ) {
            Thread.currentThread().interrupt(); // Avoid hiding the fact of interruption.
            throw new Unhandled( x ); } // Q.v. at `bin/build` for the reason.
        catch( IOException x ) { throw new Unhandled( x ); }
        finally {
            final var o = System.out;
            o.print( sourceNames.size() );
            if( capture.length() > 0 ) {
                o.println( " …" ); // Indicating the intent of compiling so many, as opposed to the fact.
                o.print( capture.toString() );
                o.flush(); }
            else o.println(); }}



    /** A path tester that always answers `true`.
      */
    public static final Predicate<Path> pathTester_true = _p -> true;



    /** Prints and flushes through standard output the beginning of a message of incremental build
      * progress.  Be sure to print the remainder of the message and terminate it with a newline.
      *
      *     @param projectPackage The proper package of the project whose software is being built,
      *       or null if the builder builder itself is being built.
      *     @param type A short name to identify the type of progress.
      */
    public static void printProgressLeader( final String projectPackage, final String type ) {
        if( !Objects.equals( projectShowingProgress, projectPackage )) {
            projectShowingProgress = projectPackage;
            System.out.println( projectPackage == null ?
              "building.Makeshift (bootstrap)" : projectPackage ); }
        System.out.print( "    " );
        System.out.print( type );
        System.out.print( ' ' );
        System.out.flush(); }



    /** The output directory of the present project.
      */
    public static final Path projectOutputDirectory = Path.of( System.getProperty("java.io.tmpdir"),
      "building.Makeshift" );



    /** The proper path of the present project.
      */
    public static final Path projectPath = toProperPath( "building.Makeshift" );



    /** Gives the simple name of the Java type proper to a source file at path `sourcePath`.
      * This assumes the restriction described at the end of §7.6 of the language specification,
      * e.g. giving type name ‘Toad’ for a path of `wet/sprocket/Toad.java`.
      *
      *     @param sourcePath The proper path of the source file.
      *     @see <a href='https://docs.oracle.com/javase/specs/jls/se15/html/jls-7.html#jls-7.6'>§7.6</a>
      *     @see #typeName(Path)
      */
    public static String simpleTypeName( final Path sourcePath ) {
        final String s = sourcePath.getFileName().toString();
        assert s.endsWith( ".java" );
        return s.substring( 0, s.length()-".java".length() ); }



    /** Whether `sourceFile` needs to be compiled or recompiled.
      *
      *     @param sourceFile The proper path of a Java source file.
      *     @param simpleTypeName The corresponding {@linkplain #simpleTypeName(Path) simple type name}.
      */
    public static boolean toCompile( final Path sourceFile, final String simpleTypeName ) {
        final Path classFile = projectOutputDirectory.resolve(
          sourceFile.resolveSibling( simpleTypeName + ".class" ));
        if( Files.exists( classFile )) {
            try {
                return getLastModifiedTime(sourceFile).compareTo(getLastModifiedTime(classFile)) >= 0; }
            catch( IOException x ) { throw new Unhandled( x ); }}
        return true; }



    /** Returns the name of the Java package at the given proper path.
      *
      *     @param properPath The relative path proper to a Java package.
      *     @throws IllegalArgumentException If `properPath` is absolute.
      */
    public static String toPackageName( final Path properPath ) {
        if( properPath.isAbsolute() ) throw new IllegalArgumentException();
        return properPath.toString().replace( separatorChar, '.' ); }



    /** Returns the proper path of the named Java package.
      *
      *     @param name The name of a Java package.
      *     @return The relative path proper to the named package.
      */
    public static Path toProperPath( final String name ) { return Path.of( toProperPathString( name )); }
      // Changing?  Sync → `toProperPath` in `bin/build`.



    /** Returns the proper path of the named Java package.
      *
      *     @param name The name of a Java package.
      *     @return The relative path proper to the named package.
      */
    public static String toProperPathString( final String name ) {
        return name.replace( '.', separatorChar ); }
        // Changing?  Sync → `toProperPathString` in `bin/build`.



    /** Gives the fully extended name of the Java type proper to a source file at path `sourcePath`.
      * This assumes the restriction described at the end of §7.6 of the language specification,
      * e.g. giving type name ‘wet.sprocket.Toad’ for a path of `wet/sprocket/Toad.java`.
      *
      *     @param sourcePath The proper path of the source file.
      *     @see <a href='https://docs.oracle.com/javase/specs/jls/se15/html/jls-7.html#jls-7.6'>§7.6</a>
      *     @see #simpleTypeName(Path)
      */
    public static String typeName( final Path sourcePath ) {
        return toPackageName(sourcePath.getParent()) + '.' + simpleTypeName(sourcePath); }



    /** Tests the validity of a `projectPath` given as the proper path of a project.
      *
      *     @throws IllegalArgumentException
      */
    public static void verify( final Path projectPath ) {
        if( projectPath.isAbsolute() ) throw new IllegalArgumentException( "Absolute `projectPath`" );
        if( projectPath.getFileName().toString().equals( "builder" )) {
          throw new IllegalArgumentException( "Project path ends with `builder`: " + projectPath ); }}
          // Simpler than trying to fathom the repercussions of allowing it, given that subdirectory
          // `builder/` is reserved for a project’s building code.



    /** Tests the validity of a `projectPackage` given as the proper package of a project.
      *
      *     @throws IllegalArgumentException
      */
    public static void verify( final String projectPackage ) {
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
    public static void verify( final String projectPackage, final Path projectPath ) {
        if( !toProperPathString(projectPackage).equals( projectPath.toString() )) {
            throw new IllegalArgumentException( "Inequivalent `projectPackage` and `projectPath`" ); }}



   // ▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀


    /** Thrown for an event that might better be handled, given a reason to do so.
      * Bootstrap equivalent of library exception
      * `<a href='http://reluk.ca/project/Java/Unhandled.java'>Java.Unhandled</a>`.
      */
    public static final class Unhandled extends RuntimeException {

        /** @see #getCause()
          */
        public  Unhandled( Exception cause ) { super( cause ); }

        /** @see #getMessage()
          */
        public  Unhandled( String message ) { super( message ); }}



   // ▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀


    /** Thrown on encountering an anomaly the user is likely in a position to correct.
      * Bootstrap equivalent of library exception
      * `<a href='http://reluk.ca/project/Java/UserError.java'>Java.UserError</a>`.
      */
    public static final class UserError extends Exception {

        /** @see #getMessage()
          */
        public UserError( String message ) { super( message ); }}



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    /** Transfers to `a` the whole of `in`.
      *
      *     @see Process#getInputStream()
      */
    private static void appendAll( final Reader in, final Appendable a ) throws IOException {
        for( ;; ) {
            int c = in.read();
            if( c == -1 ) break;
            a.append( (char)c ); }}



    /** Transfers to `a` the whole output of `process` and closes the transfer stream.
      *
      *     @see Process#getInputStream()
      */
    private static void appendAll( final Process process, final Appendable a ) throws IOException {
        final BufferedReader in = new BufferedReader( new InputStreamReader( process.getInputStream() ));
        try { appendAll( in, a ); }
        finally { in.close(); }}



    /** Proper package of the last project to show progress.
      */
    private static String projectShowingProgress = /*none yet*/""; }



                                                   // Copyright © 2020-2022  Michael Allan.  Licence MIT.
