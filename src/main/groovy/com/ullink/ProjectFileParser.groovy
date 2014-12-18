package com.ullink

import com.google.common.io.Files
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

// http://msdn.microsoft.com/en-us/library/5dy88c2e.aspx
class ProjectFileParser {
    Msbuild msbuild
    Map<String, Object> eval

    Object getProp(String key) {
        return properties[key]
    }

    def getProjectFile() {
        return properties.MSBuildProjectFullPath
    }

    def getProperties() {
        return eval.Properties
    }

    Project getProject() {
        msbuild?.project
    }

    File findProjectFile(String str) {
        findImportFile(new File(getProp('MSBuildProjectFullPath')).parentFile, str)
    }

    Logger getLogger() {
        project?.logger ?: Logging.getLogger(getClass())
    }

    File getProjectPropertyPath(String path) {
        if (getProp(path)) {
            findProjectFile(getProp(path))
        }
    }

    Collection<File> getOutputDirs() {
        ['IntermediateOutputPath', 'OutputPath'].findResults { getProjectPropertyPath(it) }
    }

    Collection<File> gatherInputs() {
        Set<File> ret = [projectFile]
        ['Compile', 'EmbeddedResource', 'None', 'Content'].each {
            eval[it].each {
                ret += findProjectFile(it.Include)
            }
        }
        eval.ProjectReference.each {
            def file = findProjectFile(it.Include).canonicalPath
            def name = Files.getNameWithoutExtension(file)
            def parser = msbuild.allProjects[name]
            ret.addAll parser.gatherInputs()
        }
        eval.Reference.each {
            if (it.HintPath) {
                ret += findProjectFile(it.HintPath)
            }
        }
        ret
    }

    static String ospath(String path) {
        path.replaceAll("\\\\|/", "\\" + System.getProperty("file.separator"))
    }

    static File findImportFile(File baseDir, String s) {
        def path = ospath(s)
        def file = new File(path)
        if (!file.isAbsolute()) {
            file = new File(baseDir, path)
        }
        file.canonicalFile
    }
}