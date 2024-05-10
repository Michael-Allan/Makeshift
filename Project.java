package Makeshift;

// Changes to this file immediately affect the next build.  Treat it as a build script.


/** The present project.  Included is a medley of resources,
  * residual odds and ends that properly fit nowhere else.
  */
public final class Project extends Bootstrap {


    private Project() {}



    /** Tests the validity of a `targetClass` given as the class of build targets for a project.
      *
      *     @throws IllegalArgumentException
      */
    public static <T extends Enum<T>> void verify( final Class<T> targetClass ) {
        Enum.valueOf( targetClass, "builder" ); } // Ensuring the presence of this mandatory target.



    /** Tests for consistency between parameters given for a project.
      * Where applicable, individually test each parameter before calling this method.
      *
      *     @param targetClass The class of the project’s build targets.
      *     @param projectPackage The proper package of the project.
      *     @throws IllegalArgumentException
      */
    public static void verify( final Class<?> targetClass, final String projectPackage ) {
        final String iBC = targetClass.getPackageName(); /* Of `BuilderBuilder.internalBuildingCode`,
          that is, according to whose API description one of the following tests must pass. */
        if( iBC.equals( projectPackage )) return;
        if( iBC.length() == projectPackage.length() + ".builder".length()
          && iBC.startsWith( projectPackage )
          && iBC.endsWith( ".builder" )) return;
        throw new IllegalArgumentException(
          "Inconsistency between `projectPackage` and `targetClass` package" ); }}



                                                        // Copyright © 2020  Michael Allan.  Licence MIT.
