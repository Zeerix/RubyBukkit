# RubyBukkit plugin init script.
# Executed before each plugin to initialize environment
#
# This is loaded directly from the RubyBukkit.jar
#
# @author Zeerix
#
# Define a plugin:
#
# Plugin.is {
#    name "MyPlugin"
#    version "0.1"
#    author "Zeerix"
#    commands :my => {
#       :description => "Describe me!",
#       :usage => "/my"
#    }
# }
#
# class MyPlugin < RubyPlugin
#    def onEnable
#       ...
#    end
# end

require 'java'
require 'rubybukkit/plugin-description'
require 'rubybukkit/plugin-class'
