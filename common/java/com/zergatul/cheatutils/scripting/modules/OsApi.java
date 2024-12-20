package com.zergatul.cheatutils.scripting.modules;

import com.zergatul.cheatutils.concurrent.TickEndExecutor;
import com.zergatul.cheatutils.scripting.AdvancedApi;
import com.zergatul.scripting.MethodDescription;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@AdvancedApi
public class OsApi {

    @MethodDescription("""
            Starts external program and returns exit code
            """)
    public CompletableFuture<Integer> execute(String path) {
        Process process;
        try {
            process = new ProcessBuilder(path).start();
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }

        return process.onExit().thenApplyAsync(Process::exitValue, TickEndExecutor.instance);
    }

    @MethodDescription("""
            Starts external program and returns exit code
            """)
    public CompletableFuture<Integer> execute(String path, String[] arguments) {
        List<String> list = new ArrayList<>();
        list.add(path);
        list.addAll(Arrays.asList(arguments));

        Process process;
        try {
            process = new ProcessBuilder(list).start();
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }

        return process.onExit().thenApplyAsync(Process::exitValue, TickEndExecutor.instance);
    }
}