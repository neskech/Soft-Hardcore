// package net.ness.softhardcore.component;

// import dev.onyxstudios.cca.api.v3.component.ComponentKey;
// import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
// import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
// import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
// import dev.onyxstudios.cca.api.v3.entity.RespawnCopyStrategy;
// import net.minecraft.util.Identifier;

// public class MyComponents implements EntityComponentInitializer {
//     public static final ComponentKey<LivesComponent> LIVES_KEY =
//             ComponentRegistry.getOrCreate(Identifier.of("softhardcore", "lives"), LivesComponent.class);

//     @Override
//     public void registerEntityComponentFactories(EntityComponentFactoryRegistry entityComponentFactoryRegistry) {
//         entityComponentFactoryRegistry.registerForPlayers(LIVES_KEY, LivesComponent::new, RespawnCopyStrategy.ALWAYS_COPY);
//     }
// }
