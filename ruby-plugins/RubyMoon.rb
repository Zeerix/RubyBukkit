# This is an example ChunkGenerator plugin for RubyBukkit
# based on Dinnerbone's BukkitFullOfMoon (https://github.com/Dinnerbone/BukkitFullOfMoon)

Plugin.is {
    name "RubyMoon"
    version "0.2"
    author "Zeerix"
    description "Ruby version of the example custom world generator for Bukkit"
    commands :moon => {
        :description => "Sends you to the moon!",
        :usage => "/moon"
    }
}

# global imports
import 'org.bukkit.Material'
import 'org.bukkit.generator.BlockPopulator'

# populators
import 'org.bukkit.util.BlockVector'
import 'org.bukkit.util.Vector'

class MoonCraterPopulator < BlockPopulator 
    CraterChance    = 45 # out of 100
    BigCraterChance = 10 # out of 100
    MinCraterSize   = 3
    SmallCraterSize = 8
    BigCraterSize   = 16

    def populate(world, random, source)
        if random.nextInt(100) <= CraterChance then
            centerX = (source.getX << 4) + random.nextInt(16)
            centerZ = (source.getZ << 4) + random.nextInt(16)
            centerY = world.getHighestBlockYAt(centerX, centerZ)
            center = BlockVector.new(centerX, centerY, centerZ)

            if random.nextInt(100) <= BigCraterChance then
                radius = random.nextInt(BigCraterSize - MinCraterSize + 1) + MinCraterSize
            else
                radius = random.nextInt(SmallCraterSize - MinCraterSize + 1) + MinCraterSize
            end
            
            for x in -radius..radius do
                for y in -radius..radius do
                    for z in -radius..radius do
                        position = center.clone.add( Vector.new(x, y, z) )
                        if center.distance(position) <= radius + 0.5 then
                            world.getBlockAt(position.toLocation(world)).setType(Material::AIR)
                        end
                    end
                end
            end
        end
    end 
end

import 'org.bukkit.block.BlockFace'

class FlagPopulator < BlockPopulator 
    FlagChance = 1 # out of 200
    FlagHeight = 3

    def populate(world, random, source)
        if random.nextInt(200) <= FlagChance then
            centerX = (source.getX << 4) + random.nextInt(16)
            centerZ = (source.getZ << 4) + random.nextInt(16)
            centerY = world.getHighestBlockYAt(centerX, centerZ)
            
            direction = case random.nextInt(4)
               when 0 then BlockFace::NORTH
               when 1 then BlockFace::EAST
               when 2 then BlockFace::SOUTH
               else        BlockFace::WEST
            end
            
            for y in centerY...centerY + FlagHeight do
                top = world.getBlockAt(centerX, y, centerZ)
                top.setType(Material::FENCE)
            end
            
            signBlock = top.getFace(direction)
            signBlock.setType(Material::WALL_SIGN)
            sign = signBlock.getState
            if sign.is_a? org.bukkit.block.Sign then
                data = sign.getData
                data.setFacingDirection(direction)
                sign.setLine(0, "---------|*****")
                sign.setLine(1, "---------|*****")
                sign.setLine(2, "-------------")
                sign.setLine(3, "-------------")
                sign.update(true)
            end
        end
    end
end

# generator class
import 'org.bukkit.generator.ChunkGenerator'
import 'org.bukkit.util.noise.NoiseGenerator'
import 'org.bukkit.util.noise.SimplexNoiseGenerator'
import 'org.bukkit.Location'

class MoonChunkGenerator < ChunkGenerator 
    
    def getGenerator(world)
        @generator = SimplexNoiseGenerator.new(world) if @generator == nil
        @generator
    end    
    
    def getHeight(world, x, y, variance)
        gen = getGenerator(world)
        
        result = gen.noise(x, y)
        result *= variance
        NoiseGenerator.floor(result)
    end
    
    def generate(world, random, cx, cz)
        result = Java::byte[32768].new
        for x in 0..15 do
            for z in 0..15 do
                height = getHeight(world, cx + x * 0.0625, cz + z * 0.0625, 2) + 60
                for y in 0...height do
                    result[(x*16+z) * 128 + y] = Material::SPONGE.getId                
                end
            end
        end
        result
    end
    
    def getDefaultPopulators(world)
        [ MoonCraterPopulator.new, FlagPopulator.new ]
    end
    
    def getFixedSpawnLocation(world, random)
        x = random.nextInt(200) - 100
        z = random.nextInt(200) - 100
        y = world.getHighestBlockYAt(x, z)
        Location.new(world, x, y, z)
    end
end

# plugin class
import 'org.bukkit.World'
import 'org.bukkit.event.Event'
import 'org.bukkit.entity.Player'

class RubyMoon < RubyPlugin
    WorldName = "BukkitMoon"
    
    def onEnable
        print getDescription.getFullName + " enabled!"
    end
    def onDisable; end
    
    def getMoon
        if @moon == nil then
            @moon = getServer.createWorld(WorldName, World::Environment::NORMAL, MoonChunkGenerator.new)
        end
        @moon
    end    
    
    def onCommand(sender, command, label, args)
        if sender.is_a? Player then
            sender.teleport getMoon.getSpawnLocation
        else
            sender.sendMessage "I don't know who you are!"
        end
        true
    end    
end
