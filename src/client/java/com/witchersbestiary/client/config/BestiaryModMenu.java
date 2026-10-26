package com.witchersbestiary.client.config;
import com.terraformersmc.modmenu.api.*;
public final class BestiaryModMenu implements ModMenuApi {
 @Override public ConfigScreenFactory<?> getModConfigScreenFactory(){return BestiarySettingsScreen::new;}
}
