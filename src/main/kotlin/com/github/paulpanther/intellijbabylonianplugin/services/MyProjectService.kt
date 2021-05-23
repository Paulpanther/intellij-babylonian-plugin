package com.github.paulpanther.intellijbabylonianplugin.services

import com.github.paulpanther.intellijbabylonianplugin.MyBundle
import com.intellij.openapi.project.Project

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
