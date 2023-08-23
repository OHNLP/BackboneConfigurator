package org.ohnlp.backbone.configurator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpMethod;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UpdateManager {
    public static void checkForUpdates() throws IOException {
        Scanner scanner = new Scanner(System.in);
        checkForUpdates(
                (asset) -> {
                    System.out.println("Found Update: $1 $2 (Currently Installed: $3)".replace("$1", asset.getRepo()).replace("$2", asset.getNewTag()).replace("$3", asset.getCurrTag()));
                    while (true) {
                        System.out.print("Install? (Y/N): ");
                        String input = scanner.nextLine().trim();
                        if (input.equalsIgnoreCase("Y")) {
                            return true;
                        } else if (input.equalsIgnoreCase("N")) {
                            return false;
                        }
                    }
                },
                null,
                null
        );
    }

    public static Map<String, String> getAssetList() throws IOException {
        String url = "https://github.com/OHNLP/BackboneConfigurator/blob/master/asset_manifest.json";
        File tmp = new RestTemplate().execute(url, HttpMethod.GET, null, clientHttpResponse -> {
            File ret = File.createTempFile("components", "tmp");
            StreamUtils.copy(clientHttpResponse.getBody(), new FileOutputStream(ret));
            return ret;
        });
        JsonNode included = new ObjectMapper().readTree(tmp);
        Map<String, String> repoAndAssetName = new HashMap<>();
        included.fields().forEachRemaining(e -> {
            repoAndAssetName.put(e.getKey(), e.getValue().asText());
        });
        return repoAndAssetName;
    }

    public static String getLatestRelease(String repo) {
        RestTemplate rest = new RestTemplate();
        ObjectNode resp = rest.getForObject("https://api.github.com/repos/$1/releases/latest".replace("$1", repo), ObjectNode.class);
        return resp.get("tag_name").asText();
    }

    public static File getReleaseFile(String repo, String tag, String assetName) {
        String url = "https://github.com/$1/releases/download/$2/$3".replace("$1", repo)
                .replace("$2", tag)
                .replace("$3", assetName);
        return new RestTemplate().execute(url, HttpMethod.GET, null, clientHttpResponse -> {
            File ret = File.createTempFile("download", "tmp");
            StreamUtils.copy(clientHttpResponse.getBody(), new FileOutputStream(ret));
            return ret;
        });
    }

    public static void checkForUpdates(Function<UpdatableAsset, Boolean> promptUpdatable, Map<String, String> include, Map<String, String> exclude) throws IOException {
        String version = new BufferedReader(
                new InputStreamReader(
                        Objects.requireNonNull(UpdateManager.class.getResourceAsStream("/configurator-version.txt")),
                        StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining(" ")).trim();
        Map<String, String> assets = getAssetList();
        if (include != null && include.size() > 0) {
            assets = include;
        } else if (exclude != null && exclude.size() > 0) {
            for (String repo : exclude.keySet()) {
                assets.remove(repo);
            }
        }
        Map<String, String> currInstalledVersions = new HashMap<>();
        currInstalledVersions.put("OHNLP/BackboneConfigurator", version);
        File stackVersionFile = new File("ohnlptk_versions.json");
        if (stackVersionFile.exists()) {
            try {
                currInstalledVersions = new ObjectMapper().readValue(stackVersionFile, new TypeReference<>() {
                });
            } catch (Throwable t) {
                System.out.println("Fresh OHNLP Toolkit Install or the Versioning file is out of date/corrupted, doing a full re-install! " +
                        "Please back up your configurations if existing before proceeding");
            }
        }
        Map<String, String> assetsUpdatable = new HashMap<>();
        Map<String, String> finalCurrInstalledVersions = currInstalledVersions;
        assets.forEach((repo, asset) -> {
            try {
                String tag = getLatestRelease(repo);
                if (!finalCurrInstalledVersions.containsKey(repo) || !finalCurrInstalledVersions.get(repo).equals(tag)) {
                    assetsUpdatable.put(repo, asset);
                }
            } catch (Throwable ignored) {
            }
        });
        assetsUpdatable.forEach((repo, asset) -> {
            String tag = getLatestRelease(repo);
            String currTag = finalCurrInstalledVersions.getOrDefault(repo, "NONE");
            if (promptUpdatable.apply(new UpdatableAsset(repo, currTag, tag))) {
                try {
                    File tmp = getReleaseFile(repo, tag, asset);
                    unzipToDir(tmp, new File("."));
                    finalCurrInstalledVersions.put(repo, tag);
                } catch (Throwable t) {
                    RuntimeException re = new RuntimeException("Failed to update " + repo + " " + tag, t);
                    re.printStackTrace();
                }
            }
        });
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(stackVersionFile, finalCurrInstalledVersions);
    }

    public static void unzipToDir(File zipFile, File targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            // - Now actually copy the resources over
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String pathRelative = entry.getName();
                File pathInTmp = new File(targetDir, pathRelative);
                byte[] contents = zis.readAllBytes();
                try (FileOutputStream fos = new FileOutputStream(pathInTmp)) {
                    fos.write(contents);
                    fos.flush();
                }
            }
        }
    }

    public static class UpdatableAsset {
        String repo;
        String currTag;
        String newTag;

        public UpdatableAsset(String repo, String currTag, String newTag) {
            this.repo = repo;
            this.currTag = currTag;
            this.newTag = newTag;
        }

        public String getRepo() {
            return repo;
        }

        public void setRepo(String repo) {
            this.repo = repo;
        }

        public String getCurrTag() {
            return currTag;
        }

        public void setCurrTag(String currTag) {
            this.currTag = currTag;
        }

        public String getNewTag() {
            return newTag;
        }

        public void setNewTag(String newTag) {
            this.newTag = newTag;
        }
    }
}
