package com.witchersbestiary.config;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.*;

/** Updates only nine logical properties, preserving unrelated bytes, comments and continued values. */
public final class ProgressiveConfigFile {
 private ProgressiveConfigFile(){}
 public static ProgressiveSettings save(Path path,ProgressiveOptions options)throws IOException{
  String text=Files.readString(path,StandardCharsets.ISO_8859_1);var original=new Properties();original.load(new StringReader(text));
  var updates=options.properties();var remaining=new TreeSet<>(updates.stringPropertyNames());var output=new StringBuilder();String newline=text.contains("\r\n")?"\r\n":"\n";
  String[] lines=text.split("(?<=\n)",-1);var logical=new StringBuilder();
  for(String line:lines){logical.append(line);String raw=line.replaceAll("[\r\n]+$","");int slashes=0;for(int i=raw.length()-1;i>=0&&raw.charAt(i)=='\\';i--)slashes++;
   if(slashes%2!=0&&!line.isEmpty())continue;
   var chunk=new Properties();chunk.load(new StringReader(logical.toString()));String key=chunk.stringPropertyNames().stream().filter(updates::containsKey).findFirst().orElse(null);
   if(key==null)output.append(logical);else{output.append(key).append('=').append(updates.getProperty(key)).append(newline);remaining.remove(key);}logical.setLength(0);
  }
  output.append(logical);if(!remaining.isEmpty()&&!output.isEmpty()&&output.charAt(output.length()-1)!='\n')output.append(newline);
  for(String key:remaining)output.append(key).append('=').append(updates.getProperty(key)).append(newline);
  Path temp=Files.createTempFile(path.toAbsolutePath().getParent(),"witchersbestiary-",".tmp");
  try{Files.writeString(temp,output,StandardCharsets.ISO_8859_1);try{Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(temp);}
  return options.applyTo(ProgressiveSettings.read(original));
 }
}
