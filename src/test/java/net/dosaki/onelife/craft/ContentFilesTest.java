package net.dosaki.onelife.craft;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ContentFilesTest {

    @Test
    void everyInstalledFileIsBundled() throws Exception {
        for (String file : ContentInstaller.FILES) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("craftengine/onelife/" + file)) {
                assertNotNull(in, "missing resource " + file);
            }
        }
    }

    @Test
    void modelUsesTheTexture() throws Exception {
        String path = "craftengine/onelife/resourcepack/assets/onelife/models/item/gravestone.json";
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            JsonObject model = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            assertTrue(model.getAsJsonObject("textures").get("stone").getAsString().equals("onelife:item/gravestone"));
            assertTrue(model.getAsJsonArray("elements").size() >= 3);
        }
    }

    @Test
    void textureIsAPng() throws Exception {
        String path = "craftengine/onelife/resourcepack/assets/onelife/textures/item/gravestone.png";
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            byte[] header = in.readNBytes(8);
            assertTrue(header[1] == 'P' && header[2] == 'N' && header[3] == 'G');
        }
    }
}
