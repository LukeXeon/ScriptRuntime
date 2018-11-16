## ScriptRuntime
* This is a low-level script runtime that is nearly identical to the syntax of the C programming language and provides most commonly used C runtime libraries and posix interfaces, and memory allocation is managed. 
* You can execute the script code in sub process or on a thread of the current process, and when the script code is executed in the current process, you can also set it to a Java layer callback that supports Java's base and String types. 
* The goal of this project is to solve the compile and execute C code in android is difficult, because the ARM branch of GCC has fully 20 MB, and the project all the binary added up to less than 300 KB, it is small but all-sided. So it is very suitable for embedding android applications, this for the development of compact android C programming language IDE provides may.
* Also, changing it to jni provides another possibility because it contains many underlying system calls.

## Dependency

Android target API level >=19.
## How to

To get a Git project into your build:

### Step 1. Add the JitPack repository to your build file


gradle:

Add it in your root build.gradle at the end of repositories:

	allprojects {
		repositories {
			...
			maven { url 'https://www.jitpack.io' }
		}
	}
	

### Step 2. Add the dependency

	dependencies {
	        implementation 'com.github.LukeXeon:ScriptRuntime:0.1.1'
	}

## Related projects
* [picoc](https://gitlab.com/zsaleeba/picoc) This project is a secondary development based on picoc, which is the android branch of picoc.
* [linenoise NG](https://github.com/arangodb/linenoise-ng) Using linenoise NG to replace the GNU readline library originally used by picoc reduces the size of the binary and reduces the dependency, making the program run better on mobile platforms.
* [Android-C-Interpreter](https://github.com/sanyuankexie/Android-C-Interpreter) The jni layer source can be found here, but it is not necessarily a stable version, and the jni layer source is built using Visual Stdio.

## Licence

        BSD 3-Clause License
        
        Copyright (c) 2018, Luke
        All rights reserved.
        
        Redistribution and use in source and binary forms, with or without
        modification, are permitted provided that the following conditions are met:
        
        * Redistributions of source code must retain the above copyright notice, this
          list of conditions and the following disclaimer.
        
        * Redistributions in binary form must reproduce the above copyright notice,
          this list of conditions and the following disclaimer in the documentation
          and/or other materials provided with the distribution.
        
        * Neither the name of the copyright holder nor the names of its
          contributors may be used to endorse or promote products derived from
          this software without specific prior written permission.
        
        THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ”AS IS“
        AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
        IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
        DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
        FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
        DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
        SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
        CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
        OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
        OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
