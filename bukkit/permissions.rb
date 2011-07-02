# Ruby permissions library 0.1
#
# Supports: Permissions, PermissionEx, GroupManager
#
# Usage: plugin.has(player, "permission.node")
#
# Setup:
#    require 'bukkit/permissions'
#    class MyPlugin < RubyPlugin
#       include Permissions
#       def onEnable
#          setupPermissions
#          ...
#       end
#    end
#
# @author Zeerix

class PermissionsHandler
    attr_reader :plugin
    
    def initialize(server)
        gm = server.getPluginManager.getPlugin("GroupManager")    
        pex = server.getPluginManager.getPlugin("PermissionsEx")    
        pm = server.getPluginManager.getPlugin("Permissions")    

        if gm != nil then
            @plugin = gm
            @handler = proc { |player, perm, default| gm.getWorldsHolder.getWorldPermissions(player).has(player, perm) }
        elsif pex != nil then
            @plugin = pex
            @handler = proc { |player, perm, default| pex.has(player, perm) } 
        elsif pm != nil then
            @plugin = pm
            @handler = proc { |player, perm, default| pm.getHandler.has(player, perm) }
        else
            @handler = proc { |player, perm, default| default }
        end
    end
    
    def playerHas(player, perm, default)
        @handler.call(player, perm, default)
    end
end

module Permissions
    def setupPermissions
        prefix = "["+getDescription.getName+"] "
        
        @permissionsHandler = PermissionsHandler.new(org.bukkit.Bukkit.getServer)
        plugin = @permissionsHandler.plugin        
        if plugin != nil then
            print prefix + "Permissions enabled using: " + plugin.getDescription.getFullName
        else
            print prefix + "A permission plugin isn\'t loaded."
        end
    end
    
    def has(player, perm, default = true)
        @permissionsHandler.playerHas(player, perm, default)
    end
end
