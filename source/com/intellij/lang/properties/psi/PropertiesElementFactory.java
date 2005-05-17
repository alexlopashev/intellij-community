package com.intellij.lang.properties.psi;

import com.intellij.lang.ParserDefinition;
import com.intellij.lang.properties.PropertiesFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;

/**
 * @author cdr
 */
public class PropertiesElementFactory {
  public static Property createProperty(Project project, String name, String value) {
    ParserDefinition def = PropertiesFileType.FILE_TYPE.getLanguage().getParserDefinition();
    String filename = "dummy." + PropertiesFileType.FILE_TYPE.getDefaultExtension();
    String text = escape(name) + "=" + value;
    final PropertiesFile dummyFile = (PropertiesFile)def.createFile(project, filename, text);
    return dummyFile.getProperties().get(0);
  }                   

  private static String escape(String name) {
    if (StringUtil.startsWithChar(name, '#')) {
      name = escapeChar(name, '#');
    }
    if (StringUtil.startsWithChar(name, '!')) {
      name = escapeChar(name, '!');
    }
    name = escapeChar(name, '=');
    name = escapeChar(name, ':');
    name = escapeChar(name, ' ');
    name = escapeChar(name, '\t');
    return name;
  }

  private static String escapeChar(String name, char c) {
    while (true) {
      int i = name.indexOf(c);
      if (i == -1) return name;
      if (i == 0 || name.charAt(i-1) != '\\') {
        name = name.substring(0, i) + '\\' + name.substring(i);
      }
    }
  }
}
