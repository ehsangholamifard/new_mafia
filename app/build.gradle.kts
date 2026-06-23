plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.aistudio.mafiagod.vhrqla"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

tasks.register("gitDiff") {
    doLast {
        val file = file("src/main/java/com/example/ui/screens/MainGameScreen.kt")
        val content = file.readLines()
        val stack = mutableListOf<Pair<Int, String>>()
        var inString = false
        var escaped = false
        var inBlockComment = false
        for ((lineIdx, lineText) in content.withIndex()) {
            val lineNum = lineIdx + 1
            var colIdx = 0
            var inLineComment = false
            while (colIdx < lineText.length) {
                val char = lineText[colIdx]
                if (escaped) {
                    escaped = false
                    colIdx++
                    continue
                }
                if (char == '\\') {
                    escaped = true
                    colIdx++
                    continue
                }
                if (inBlockComment) {
                    if (char == '*' && colIdx + 1 < lineText.length && lineText[colIdx + 1] == '/') {
                        inBlockComment = false
                        colIdx += 2
                    } else {
                        colIdx++
                    }
                    continue
                }
                if (inLineComment) {
                    break
                }
                if (char == '/' && colIdx + 1 < lineText.length && lineText[colIdx + 1] == '*') {
                    inBlockComment = true
                    colIdx += 2
                    continue
                }
                if (char == '/' && colIdx + 1 < lineText.length && lineText[colIdx + 1] == '/') {
                    inLineComment = true
                    break
                }
                if (char == '"') {
                    inString = !inString
                    colIdx++
                    continue
                }
                if (!inString) {
                    if (char == '{') {
                        stack.add(Pair(lineNum, lineText.trim()))
                    } else if (char == '}') {
                        if (stack.isNotEmpty()) {
                            stack.removeAt(stack.size - 1)
                        } else {
                            println("Unmatched closing brace at line $lineNum: ${lineText.trim()}")
                        }
                    }
                }
                colIdx++
            }
        }
        println("UNCLOSED_BRACES_COUNT: ${stack.size}")
        println("TOP_UNCLOSED_BRACES (oldest to newest):")
        for (i in 0 until Math.min(25, stack.size)) {
            val item = stack[i]
            println("Line ${item.first}: ${item.second}")
        }
        println("LATEST_UNCLOSED_BRACES (newest to oldest):")
        for (i in stack.size - 1 downTo Math.max(0, stack.size - 25)) {
            val item = stack[i]
            println("Line ${item.first}: ${item.second}")
        }
    }
}

tasks.register("applyFix") {
    doLast {
        val file = file("src/main/java/com/example/ui/screens/MainGameScreen.kt")
        val lines = file.readLines().toMutableList()
        var targetLineIdx = -1
        for ((idx, line) in lines.withIndex()) {
            if (line.contains("gunAlertMessage!!") && line.contains("AccentGold") && line.contains("fontSize = 11.sp")) {
                targetLineIdx = idx
                break
            }
        }
        if (targetLineIdx != -1) {
            println("Found target at line ${targetLineIdx + 1}")
            for (offset in 1..10) {
                if (targetLineIdx + offset < lines.size) {
                    println("Offset $offset: '${lines[targetLineIdx + offset].trim()}'")
                }
            }
            
            // Insert 3 closing braces right after the '}' of the 'else' block (which is targetLineIdx + 5)
            lines.add(targetLineIdx + 6, "                  }")
            lines.add(targetLineIdx + 7, "              }")
            lines.add(targetLineIdx + 8, "          }")
            file.writeText(lines.joinToString("\n"))
            println("Applied fix successfully!")
        } else {
            println("Error: Target line not found!")
        }
    }
}

tasks.register("gitStatus") {
    doLast {
        val process = ProcessBuilder("git", "status").start()
        println(process.inputStream.bufferedReader().readText())
        println(process.errorStream.bufferedReader().readText())
    }
}

tasks.register("gitDiffReal") {
    doLast {
        val process = ProcessBuilder("git", "diff").start()
        println(process.inputStream.bufferedReader().readText())
        println(process.errorStream.bufferedReader().readText())
    }
}

