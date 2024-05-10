package Makeshift;

// Changes to this file immediately affect the next build.  Treat it as a build script.

import java.nio.file.Path;
import java.util.*;

import static Makeshift.Project.UserError;
import static Makeshift.Project.toProperPath;
import static Makeshift.Project.verify;


/** Default implementation of a software builder.  It supports all the targets named in
  * `Makeshift.template.{@linkplain Makeshift.template.BuildTarget BuildTarget}`,
  * but will refuse to build any outside of `T`.
  *
  *     @param <T> The type of build targets.  The names of all its targets should comprise
  *       a subset of {@linkplain Makeshift.template.BuildTarget those supported}.
  */
public class BuilderDefault<T extends Enum<T>> implements Builder {


    /** @param targetClass The class of build targets.
      * @see projectPackage()
      */
    public BuilderDefault( final Class<T> targetClass, final String projectPackage ) {
        verify( targetClass );
        verify( projectPackage );
        verify( targetClass, projectPackage );
        this.targetClass = targetClass;
        this.projectPackage = projectPackage;
        projectPath = toProperPath( projectPackage ); }



    /** @param targetClass The class of build targets.
      * @see projectPackage()
      * @see projectPath()
      */
    public BuilderDefault( final Class<T> targetClass, final String projectPackage,
          final Path projectPath ) {
        verify( targetClass );
        verify( projectPackage );
        verify( projectPath );
        verify( projectPackage, projectPath );
        verify( targetClass, projectPackage );
        this.targetClass = targetClass;
        this.projectPackage = projectPackage;
        this.projectPath = projectPath; }



    /** Packages of Java code proper to the owning project, exclusive of building code.
      * The code comprises all `.java` files of the
      * {@linkplain Project#toProperPath(String) equivalent directories},
      * exclusive of their subdirectories.
      *
      * <p>The default implementation is a singleton set comprising the proper package
      * of the owning project.</p>
      *
      * <p>This method is not called unless the project declares build target
      * `{@linkplain Makeshift.template.BuildTarget.Java_class_files Java_class_files}`.</p>
      */
    public Set<String> JavaCode() { return Set.of( projectPackage ); } /* Packages for elements
      because they are codeable by implementers as cross-platform literals, whereas paths are not. */



    /** The proper package of the owning project.
      */
    public final String projectPackage() { return projectPackage; }


        private final String projectPackage;



    /** The proper path of the owning project.
      */
    public final Path projectPath() { return projectPath; }


        private final Path projectPath;



   // ━━━  B u i l d e r  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━


    /** @throws IllegalArgumentException If the matching target is unsupported by this implementation.
      */
    public @Override void build( final String targ ) throws UserError {
        buildTo( Builder.matchingTargetName( targ, targetClass )); }



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    /** Builds the code to the level of `target`.
      *
      *     @param target The full name of the target.
      *     @throws IllegalArgumentException If `target` is unsupported by this implementation.
      */
    protected final void buildTo( final String target ) throws UserError {
        switch( target ) {
            case "builder"          -> {} // Nothing to do, already this builder is built.
            case "Java_class_files" -> buildTo_Java_class_files();
            default -> {
                assert !isSupportDeclared( target );
                throw new IllegalArgumentException(); }}
        assert isSupportDeclared( target ); }



    /** @see Makeshift.template.BuildTarget.Java_class_files
      * @see #javacArguments()
      */
    private void buildTo_Java_class_files() throws UserError {
        final List<String> sourceNames = new ArrayList<>();
        JavaCode().forEach( pkg -> Project.addCompilableSource( sourceNames, toProperPath(pkg) ));
        if( sourceNames.size() > 0 ) {
            Project.compile( projectPackage, sourceNames, javacArguments() ); }}



    /** Additional arguments for the Java compiler.  The default implementation is an empty list.
      *
      *     @see Makeshift.template.BuildTarget.Java_class_files
      */
    protected List<String> javacArguments() { return List.of(); }



    private static boolean isSupportDeclared( final String target ) {
        boolean is = true;
        try { Makeshift.template.BuildTarget.valueOf( target ); }
        catch( IllegalArgumentException _x ) { is = false; }
        return is; }



    private final Class<T> targetClass; }



                                                   // Copyright © 2020-2021  Michael Allan.  Licence MIT.
