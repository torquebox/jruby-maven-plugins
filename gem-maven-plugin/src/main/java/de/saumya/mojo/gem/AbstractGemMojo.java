package de.saumya.mojo.gem;

import java.io.File;
import java.io.IOException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;

import de.saumya.mojo.jruby.AbstractJRubyMojo;
import de.saumya.mojo.ruby.gems.GemException;
import de.saumya.mojo.ruby.gems.GemManager;
import de.saumya.mojo.ruby.gems.GemsConfig;
import de.saumya.mojo.ruby.gems.GemsInstaller;
import de.saumya.mojo.ruby.script.GemScriptFactory;
import de.saumya.mojo.ruby.script.ScriptException;
import de.saumya.mojo.ruby.script.ScriptFactory;

/**
 */
public abstract class AbstractGemMojo extends AbstractJRubyMojo {

    /** @component role-hint="zip" */
    protected UnArchiver unzip;
    
    /** @parameter expression="${plugin}" @readonly */
    protected PluginDescriptor  plugin;

    /**
     * flag whether to include open-ssl gem or not
     * <br/>
     * Command line -Dgem.includeOpenSSL=...
     * 
     * @parameter expression="${gem.includeOpenSSL}" default-value="true"
     */
    protected boolean       includeOpenSSL;

    /**
     * flag whether to install rdocs of the used gems or not
     * <br/>
     * Command line -Dgem.installRDoc=...
     * 
     * @parameter expression="${gem.installRDoc}" default-value="false"
     */
    protected boolean         installRDoc;

    /**
     * flag whether to install ri of the used gems or not
     * <br/>
     * Command line -Dgem.installRDoc=...
     * 
     * @parameter expression="${gem.installRI}" default-value="false"
     */
    protected boolean         installRI;

    /**
     * directory of gem home to use when forking JRuby.
     * <br/>
     * Command line -Dgem.home=...
     *
     * @parameter expression="${gem.home}"
     *            default-value="${project.build.directory}/rubygems"
     */
    protected File          gemHome;

    /**
     * directory of JRuby path to use when forking JRuby.
     * <br/>
     * Command line -Dgem.path=...
     *
     * @parameter expression="${gem.path}"
     *            default-value="${project.build.directory}/rubygems"
     */
    protected File          gemPath;

    /**
     * arguments for the gem command.
     * <br/>
     * Command line -Dgem.args=...
     *
     * @parameter default-value="${gem.args}"
     */
    protected String        gemArgs;

    /**
     * directory of JRuby bin path to use when forking JRuby.
     * <br/>
     * Command line -Dgem.binDirectory=...
     *
     * @parameter expression="${gem.binDirectory}"
     */
    protected File          binDirectory;

    /**
     * flag to indicate to setup jruby's native support for C-extensions
     * <br/>
     * Command line -Dgem.supportNative=...
     * 
     * @parameter expression="${gem.supportNative}" default-value="false"
     */
    protected boolean        supportNative;
    
    /** @component */
    protected GemManager    manager;

    protected GemsConfig    gemsConfig;

    protected GemsInstaller gemsInstaller;

    @Override
    protected ScriptFactory newScriptFactory(Artifact artifact) throws MojoExecutionException {
        if (this.project.getBasedir() == null) {
            this.gemHome = new File(this.gemHome.getAbsolutePath()
                    .replace("/${project.basedir}/", "/"));
            this.gemPath = new File(this.gemPath.getAbsolutePath()
                    .replace("/${project.basedir}/", "/"));
        }

        this.gemsConfig = new GemsConfig();
        this.gemsConfig.setGemHome(this.gemHome);
        this.gemsConfig.addGemPath(this.gemPath);

        try {
            final GemScriptFactory factory = new GemScriptFactory(this.logger,
                    this.classRealm,
                    artifact.getArtifactId().equals(JRUBY_CORE)? null: artifact.getFile(),
                    artifact.getArtifactId().equals(JRUBY_CORE)? retrieveStdlibArtifact().getFile(): artifact.getFile(),
                    this.project.getTestClasspathElements(),
                    this.jrubyFork, 
                    this.gemsConfig);
            if(supportNative){
                factory.addJvmArgs("-Djruby.home=" + setupNativeSupport().getAbsolutePath());
            }
            return factory;
        }
        catch (final DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("could not resolve jruby", e);
        }
        catch (final ScriptException e) {
            throw new MojoExecutionException("could not initialize script factory",
                    e);
        }
        catch (final IOException e) {
            throw new MojoExecutionException("could not initialize script factory",
                    e);
        }
    }

    private File setupNativeSupport() throws MojoExecutionException {
        File target = new File(this.project.getBuild().getDirectory());
        File jrubyDir = new File(target, "jruby-" + jrubyVersion);
        if (!jrubyDir.exists()){
            Artifact dist = manager.createArtifact("org.jruby",
                                                   "jruby-dist",
                                                   jrubyVersion,
                                                   "bin",
                                                   "zip");
            try {
                manager.resolve(dist,
                                localRepository,
                                project.getRemoteArtifactRepositories());
            }
            catch (final GemException e) {
                throw new MojoExecutionException("could not setup jruby distribution for native support",
                        e);
            }
            if (jrubyVerbose) {
                getLog().info("unzip " + dist.getFile());
            }
            target.mkdirs();
            unzip.setSourceFile(dist.getFile());
            unzip.setDestDirectory(target);
            try {
                unzip.extract();
                new File(target, "jruby-" + jrubyVersion + "/bin/jruby").setExecutable(true);
            }
            catch (ArchiverException e) {
                throw new MojoExecutionException("could unzip jruby distribution for native support",
                        e);
            }
        }
        return jrubyDir;
    }

    @Override
    protected void executeJRuby() throws MojoExecutionException,
            MojoFailureException, IOException, ScriptException {
                this.gemsConfig.setAddRdoc(this.installRDoc);
        this.gemsConfig.setAddRI(this.installRI);
        this.gemsConfig.setBinDirectory(this.binDirectory);
        // this.gemsConfig.setUserInstall(userInstall);
        // this.gemsConfig.setSystemInstall(systemInstall);
        this.gemsConfig.setSkipJRubyOpenSSL(!this.includeOpenSSL);

        this.gemsInstaller = new GemsInstaller(this.gemsConfig,
                this.factory,
                this.manager);

        try {
            // install the gem dependencies from the pom
            this.gemsInstaller.installPom(this.project, this.localRepository);

            // has the plugin gem dependencies ?
            boolean hasGems = false;
            for(Artifact artifact: plugin.getArtifacts()){
                if (artifact.getType().contains("gem")){
                    hasGems = true;
                    break;
                }
            }
            // install the gems for the plugin
            File pluginGemHome = new File(this.gemsConfig.getGemHome().getAbsolutePath() + "-" + plugin.getArtifactId());
            pluginGemHome.mkdirs();
            if (hasGems){
                // use a common bindir, i.e. the one from the configured gemHome
                // remove default by setting it explicitly
                this.gemsConfig.setBinDirectory(this.gemsConfig.getBinDirectory());
                File home = this.gemsConfig.getGemHome();
                this.gemsConfig.setGemHome(pluginGemHome);
                this.gemsConfig.addGemPath(this.gemsConfig.getGemHome());

                this.gemsInstaller.installGems(this.project,
                                               this.plugin.getArtifacts(), 
                                               this.localRepository, 
                                               this.project.getPluginArtifactRepositories());

                this.gemsConfig.setGemHome(home);
            }
        }
        catch (final GemException e) {
            throw new MojoExecutionException("error in installing gems", e);
        }

        // add the gems to the test-classpath
        for(File path: this.gemsConfig.getGemPath()){
            Resource resource = new Resource();
            resource.setDirectory(path.getAbsolutePath());
            project.getBuild().getTestResources().add(resource);
        }

        try {

            executeWithGems();

        }
        catch (final GemException e) {
            throw new MojoExecutionException("error in executing with gems", e);
        }
    }

    abstract protected void executeWithGems() throws MojoExecutionException,
            ScriptException, GemException, IOException, MojoFailureException;

}
