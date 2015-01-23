package com.sijobe.spc.updater;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;

import org.apache.http.client.methods.HttpGet;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.sijobe.spc.core.Constants;
import com.sijobe.spc.core.IServerAboutToStart;
import com.sijobe.spc.util.Settings;
import com.sijobe.spc.util.SettingsManager;
import com.sijobe.spc.wrapper.MinecraftServer;

/**
 * converts the settings being saved by usernames to uuids
 * 
 * @author aucguy
 * @version 1.0
 */
public class UsernameToUuid implements IServerAboutToStart, Runnable {

   enum UpdateState {
      DONE, UPDATING;

      /**
       * gets the state with the given name
       * 
       * @param text
       * @return
       */
      UpdateState get(String text) {
         for (UpdateState state : UpdateState.values()) {
            if (state.name().equalsIgnoreCase(text)) {
               return state;
            }
         }
         return null;
      }
   }

   protected static final String GLOBAL_SETTINGS = "global";
   protected static final String FORMAT = "format";
   protected static final String UPDATE_STATE = "updateState";
   protected static final String MOJANG_URL = "https://api.mojang.com/users/profiles/minecraft/";
   protected static final String URL_POSTFIX = "?at=0"; // in case of settings
                                                        // update after renames
   protected static final Settings defaultSettings = new Settings();

   static {
      defaultSettings.set(FORMAT, 0);
      defaultSettings.set(UPDATE_STATE, UpdateState.DONE.name());
   }

   /**
    * the setings manager
    */
   SettingsManager settingsManager;
   Settings globals;
   /**
    * the <minecraft>/saves/<world>/spc/players-uuid/ directory
    */
   File uuidDir;
   /**
    * the <minecraft>/saves/<world>/spc/ directory
    */
   File spcDir;
   /**
    * the last http request
    */
   Calendar lastRequest;

   @Override
   public boolean isEnabled() {
      return true;
   }

   @Override
   public void init(Object... params) {
   }

   @Override
   public void onServerAboutToStart() {
      this.run();
   }

   /**
    * sees if the settings need to be updated and if so update them.
    */
   @Override
   public void run() {
      System.out.println("running conversion...");
      try {
         this.spcDir = new File(MinecraftServer.getWorldDirectory(), "spc");
         this.settingsManager = new SettingsManager(this.spcDir,
               defaultSettings);
         this.globals = this.settingsManager.load(GLOBAL_SETTINGS);

         if (globals.getFloat(FORMAT, 0) < Constants.CURRENT_FORMAT) {
            this.convert();
         }

         this.globals.set(FORMAT, 1);
         this.globals.save();
      } catch (Throwable error) {
         System.err.println("exception updating settings format");
         error.printStackTrace();
      }
   }

   /**
    * updates the settings for this world
    * 
    * @throws IOException
    */
   public void convert() throws IOException {
      System.out.println("converting uuids");
      File usernameDir = new File(new File(MinecraftServer.getWorldDirectory(),
            "spc"), "players");
      if (!usernameDir.exists() || usernameDir.isFile()) {
         return;
      }

      this.globals.set(UPDATE_STATE, UpdateState.UPDATING.name());
      this.globals.save();
      this.uuidDir = new File(this.spcDir, "players-uuid");

      for (File file : usernameDir.listFiles()) {
         String name = file.getName();
         if (name.endsWith(SettingsManager.DEFAULT_EXTENSION)) {
            this.updateSetting(file);
         }
      }
      this.lastRequest = null;
      this.globals.set(UPDATE_STATE, UpdateState.DONE.name());
      this.globals.save();
   }

   /**
    * updates a setting file
    * 
    * @param oldSettings - the old settings file that used usernames
    * @throws IOException
    */
   protected void updateSetting(File oldSettings) throws IOException {
      String username = oldSettings.getName().substring(
            0,
            oldSettings.getName().length()
                  - SettingsManager.DEFAULT_EXTENSION.length());
      System.out.println("converting settings for " + username);
      URL url = new URL(MOJANG_URL + username + URL_POSTFIX);

      if (this.lastRequest != null) {
         Calendar until = (Calendar) this.lastRequest.clone();
         until.add(Calendar.MILLISECOND, 1010);
         waitUntil(until);
      }
      this.lastRequest = Calendar.getInstance();
      String content = getHttpResponse(url, 4096, 10000);
      if (content == null) {
         return;
      }
      UuidResponse response = parseJson(UuidResponse.class, content);

      if (response.error != null) {
         System.err.println("error getting uuid from mojang: " + response.error
               + " "
               + (response.errorMessage != null ? response.errorMessage : ""));
         return;
      }

      if (response.id == null) {
         System.err.println("id is null");
         return;
      }

      if (response.name == null) {
         System.err.println("name is null");
      }

      File newSettings = new File(this.uuidDir, response.id
            + SettingsManager.DEFAULT_EXTENSION);
      copyFile(oldSettings, newSettings);
   }

   /**
    * uses the http protocol and returns the server's response
    * 
    * @param url - the resources' URL
    * @param maxLen - the maximum length of the response
    * @param timeout - the maximum waiting time for the resource
    * @return - the response of the server
    * @throws IOException
    */
   public static String getHttpResponse(URL url, int maxLen, int timeout)
         throws IOException {
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setReadTimeout(timeout);
      InputStream stream = connection.getInputStream();
      byte[] data = new byte[maxLen];
      int read = 0;

      while ((read += stream.read(data, read, maxLen - read)) > 0) {
      }

      if (connection.getResponseCode() != 200) {
         System.err.println("failed to get response. status: "
               + connection.getResponseCode());
         return null;
      }
      return new String(data).trim();
   }

   /**
    * copies a file
    * 
    * @param src - the file to be copied
    * @param dst - the file that is created or overwritten
    * @throws IOException
    */
   public static void copyFile(File src, File dst) throws IOException {
      InputStream originalFile = new FileInputStream(src);
      dst.getParentFile().mkdir();
      OutputStream newFile = new FileOutputStream(dst);
      byte[] contents = new byte[originalFile.available()];
      originalFile.read(contents);
      newFile.write(contents);
      originalFile.close();
      newFile.close();
   }

   /**
    * parses JSON
    * 
    * @param type - the type to unserialize
    * @param content - the JSON string
    * @return - the unserialized object
    */
   public static <T> T parseJson(Class<T> type, String content) {
      Gson gson = new Gson();
      JsonReader reader = new JsonReader(new StringReader(content));
      reader.setLenient(true);
      return gson.fromJson(reader, type);
   }

   /**
    * waits until the specified time
    * 
    * @param time - the time to wait until
    */
   public static void waitUntil(Calendar time) {
      while (Calendar.getInstance().before(time)) {
      }
   }
}
