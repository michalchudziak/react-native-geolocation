require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name         = "react-native-geolocation"
  s.version      = package['version']
  s.summary      = package['description']
  s.license      = package['license']

  s.authors      = package['author']
  s.homepage     = package['homepage']
  s.platforms     = { :ios => '9.0', :visionos => '1.0' }

  s.source       = { :git => "https://github.com/react-native-community/react-native-geolocation.git", :tag => "v#{s.version}" }
  s.source_files  = "ios/**/*.{h,m,mm}"

  s.frameworks = 'CoreLocation'

  if defined?(install_modules_dependencies()) != nil
    install_modules_dependencies(s)
  else
    s.dependency "React-Core"
  end
end
