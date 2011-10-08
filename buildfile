# Generated by Buildr 1.4.6, change to your liking
require 'buildr/scala'

# Version number for this release
VERSION_NUMBER = "0.2.0"
# Group identifier for your projects
GROUP = "echo"
COPYRIGHT = "Callum Stott 2011"

# Specify Maven 2.0 remote repositories here, like this:
repositories.remote << "http://www.ibiblio.org/maven2/"

Project.local_task :typeset
Project.local_task :wipe
Project.local_task :examples
Project.local_task :console

define "echo" do

  project.version = VERSION_NUMBER
  project.group = GROUP
  manifest["Implementation-Vendor"] = COPYRIGHT

  package :jar
  test.using :specs
  
  task :typeset do
    FileUtils.makedirs('target/pdfs') unless File.exists?('target/pdfs')
    system 'xelatex -output-directory=target/pdfs src/report/*.tex'
  end
  
  task :examples => :package do
    FileUtils.makedirs('examples/example_lib') unless File.exists?('examples/example_lib')
    system 'rm examples/example_lib/*.jar' 
    system "cp target/echo-#{VERSION_NUMBER}.jar examples/example_lib/echo-#{VERSION_NUMBER}.jar"
    system 'cd examples/button && buildr'
    system 'cd examples/color && buildr'
    system 'buildr clean'
  end
  
  task :wipe => :clean do
    system 'cd examples/button && buildr clean'
    system 'cd examples/color && buildr clean'  
  end
  
  task :console => :package do
    system 'scala -classpath target:target/classes'
  end
end