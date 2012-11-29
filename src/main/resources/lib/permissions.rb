# Ruby permissions library 0.2
#
# Supports: Permissions, PermissionEx, GroupManager
#
# Usage:
#    player.has("permission.node")
#    - or -
#    Permissions.playerHas(player, "permissons.node")
#
# Setup:
#    require 'lib/permissions'
#
# @author Zeerix

class Permissions
    # singleton
    def self.instance
        @permissions = Permissions.new if @permissions == nil
        @permissions     
    end
    
    def self.playerHas(player, perm, default); instance.playerHas(player, perm, default); end
    def self.plugin; instance.plugin; end

    def self.pluginName
        plugin.getDescription.getFullName if plugin != nil
    end
        
    # the handler
    attr_reader :plugin
    
    def initialize
        server = org.bukkit.Bukkit.getServer
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

module PlayerPermissions
    def has(name, default = false)
        Permissions.playerHas(self, name, default)    
    end
end 

JavaUtilities.extend_proxy("org.bukkit.entity.Player") {
    include PlayerPermissions
}
