java -cp /home/kristian/projects/jruby-maven-plugins/gem-maven-plugin/src/it/initialize/target/test-classes:/home/kristian/projects/jruby-maven-plugins/gem-maven-plugin/src/it/initialize/target/classes -client -Xbootclasspath/a:/home/kristian/.m2/repository/org/jruby/jruby-complete/1.5.2/jruby-complete-1.5.2.jar org.jruby.Main -e "load('jar:file:/home/kristian/.m2/repository/org/jruby/jruby-complete/1.5.1/jruby-complete-1.5.1.jar!/META-INF/jruby.home/bin/gem')" -- install --no-rdoc --no-ri --no-user-install -l /home/kristian/.m2/repository/rubygems/activesupport/2.3.5/activesupport-2.3.5.gem /home/kristian/.m2/repository/rubygems/activeresource/2.3.5/activeresource-2.3.5.gem /home/kristian/.m2/repository/rubygems/activerecord/2.3.5/activerecord-2.3.5.gem /home/kristian/.m2/repository/rubygems/jruby-openssl/0.7/jruby-openssl-0.7.gem /home/kristian/.m2/repository/rubygems/rack/1.0.1/rack-1.0.1.gem /home/kristian/.m2/repository/rubygems/actionpack/2.3.5/actionpack-2.3.5.gem /home/kristian/.m2/repository/rubygems/rake/0.8.7/rake-0.8.7.gem /home/kristian/.m2/repository/rubygems/actionmailer/2.3.5/actionmailer-2.3.5.gem /home/kristian/.m2/repository/rubygems/rails/2.3.5/rails-2.3.5.gem
