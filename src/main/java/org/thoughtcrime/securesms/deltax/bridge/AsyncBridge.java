package org.thoughtcrime.securesms.deltax.bridge;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.CoerceLuaToJava;

public class AsyncBridge {
  private final ExecutorService executor =
      Executors.newCachedThreadPool(
          r -> {
            Thread t = new Thread(r, "DeltaX-Async-" + r.hashCode());
            t.setDaemon(true);
            return t;
          });

  public void register(Globals globals) {
    globals.set(
        "async",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue func) {
            org.luaj.vm2.LuaFunction luaFunc = func.checkfunction();
            CompletableFuture<Object> future =
                CompletableFuture.supplyAsync(
                    () -> {
                      try {
                        return luaFunc.call();

                      } catch (Exception e) {
                        throw new CompletionException(e);
                      }
                    },
                    executor);
            return CoerceJavaToLua.coerce(future);
          }
        });
    globals.set(
        "asyncWithDelay",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue delayMs, LuaValue func) {
            org.luaj.vm2.LuaFunction luaFunc = func.checkfunction();
            long delay = delayMs.tolong();
            CompletableFuture<Object> future =
                CompletableFuture.supplyAsync(
                    () -> {
                      try {
                        Thread.sleep(delay);
                        return luaFunc.call();

                      } catch (Exception e) {
                        throw new CompletionException(e);
                      }
                    },
                    executor);
            return CoerceJavaToLua.coerce(future);
          }
        });
    globals.set(
        "thenApply",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue futureObj, LuaValue func) {
            CompletableFuture<?> future =
                (CompletableFuture) futureObj.checkuserdata(CompletableFuture.class);
            org.luaj.vm2.LuaFunction luaFunc = func.checkfunction();
            CompletableFuture<Object> newFuture =
                future.thenApply(
                    result ->
                        CoerceLuaToJava.coerce(
                            luaFunc.call(CoerceJavaToLua.coerce(result)), Object.class));
            return CoerceJavaToLua.coerce(newFuture);
          }
        });
    globals.set(
        "thenAccept",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue futureObj, LuaValue func) {
            CompletableFuture<?> future =
                (CompletableFuture) futureObj.checkuserdata(CompletableFuture.class);
            org.luaj.vm2.LuaFunction luaFunc = func.checkfunction();
            CompletableFuture<Void> newFuture =
                future.thenAccept(result -> luaFunc.call(CoerceJavaToLua.coerce(result)));
            return CoerceJavaToLua.coerce(newFuture);
          }
        });
    globals.set(
        "thenRun",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue futureObj, LuaValue func) {
            CompletableFuture<?> future =
                (CompletableFuture) futureObj.checkuserdata(CompletableFuture.class);
            org.luaj.vm2.LuaFunction luaFunc = func.checkfunction();
            CompletableFuture<Void> newFuture = future.thenRun(() -> luaFunc.call());
            return CoerceJavaToLua.coerce(newFuture);
          }
        });
    globals.set(
        "exceptionHandler",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue futureObj, LuaValue func) {
            CompletableFuture<Object> future =
                (CompletableFuture) futureObj.checkuserdata(CompletableFuture.class);
            org.luaj.vm2.LuaFunction luaFunc = func.checkfunction();
            CompletableFuture<Object> newFuture =
                future.exceptionally(
                    throwable -> {
                      luaFunc.call(CoerceJavaToLua.coerce(throwable));
                      return null;
                    });
            return CoerceJavaToLua.coerce(newFuture);
          }
        });
    globals.set(
        "allOf",
        new VarArgFunction() {
          @Override
          public Varargs invoke(Varargs args) {
            CompletableFuture<?>[] futures = new CompletableFuture<?>[args.narg()];
            for (int i = 1; i <= args.narg(); i++) {
              futures[i - 1] = (CompletableFuture) args.checkuserdata(i, CompletableFuture.class);
            }
            return CoerceJavaToLua.coerce(CompletableFuture.allOf(futures));
          }
        });
    globals.set(
        "anyOf",
        new VarArgFunction() {
          @Override
          public Varargs invoke(Varargs args) {
            CompletableFuture<?>[] futures = new CompletableFuture<?>[args.narg()];
            for (int i = 1; i <= args.narg(); i++) {
              futures[i - 1] = (CompletableFuture) args.checkuserdata(i, CompletableFuture.class);
            }
            return CoerceJavaToLua.coerce(CompletableFuture.anyOf(futures));
          }
        });
    globals.set(
        "await",
        new VarArgFunction() {
          @Override
          public Varargs invoke(Varargs args) {
            CompletableFuture<?> future =
                (CompletableFuture) args.checkuserdata(1, CompletableFuture.class);
            long timeout = args.narg() >= 2 ? args.tolong(2) : 0L;
            try {
              Object result =
                  timeout > 0 ? future.get(timeout, TimeUnit.MILLISECONDS) : future.get();
              return CoerceJavaToLua.coerce(result);

            } catch (Exception e) {
              throw new LuaError(e);
            }
          }
        });
    globals.set(
        "isDone",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue futureObj) {
            CompletableFuture<?> future =
                (CompletableFuture) futureObj.checkuserdata(CompletableFuture.class);
            return LuaValue.valueOf(future.isDone());
          }
        });
    globals.set(
        "isCancelled",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue futureObj) {
            CompletableFuture<?> future =
                (CompletableFuture) futureObj.checkuserdata(CompletableFuture.class);
            return LuaValue.valueOf(future.isCancelled());
          }
        });
    globals.set(
        "cancelFuture",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue futureObj) {
            CompletableFuture<?> future =
                (CompletableFuture) futureObj.checkuserdata(CompletableFuture.class);
            return LuaValue.valueOf(future.cancel(true));
          }
        });
    globals.set(
        "newExecutor",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue name) {
            String threadName = name.tojstring();
            ExecutorService exec =
                Executors.newCachedThreadPool(
                    r -> {
                      Thread t = new Thread(r, "DeltaX-Pool-" + threadName);
                      t.setDaemon(true);
                      return t;
                    });
            return CoerceJavaToLua.coerce(exec);
          }
        });
    globals.set(
        "newFixedPool",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue name, LuaValue threads) {
            String threadName = name.tojstring();
            int nThreads = threads.checkint();
            ExecutorService exec =
                Executors.newFixedThreadPool(
                    nThreads,
                    r -> {
                      Thread t = new Thread(r, "DeltaX-Pool-" + threadName);
                      t.setDaemon(true);
                      return t;
                    });
            return CoerceJavaToLua.coerce(exec);
          }
        });
    globals.set(
        "submitToExecutor",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue execObj, LuaValue func) {
            ExecutorService exec = (ExecutorService) execObj.checkuserdata(ExecutorService.class);
            org.luaj.vm2.LuaFunction luaFunc = func.checkfunction();
            CompletableFuture<LuaValue> future =
                CompletableFuture.supplyAsync(() -> luaFunc.call(), exec);
            return CoerceJavaToLua.coerce(future);
          }
        });
    globals.set(
        "sleep",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue millis) {
            try {
              Thread.sleep(millis.tolong());
              return LuaValue.TRUE;

            } catch (InterruptedException e) {
              return LuaValue.FALSE;
            }
          }
        });
    globals.set(
        "newAtomicInt",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue initial) {
            return CoerceJavaToLua.coerce(new AtomicInteger(initial.checkint()));
          }
        });
    globals.set(
        "newAtomicLong",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue initial) {
            return CoerceJavaToLua.coerce(new AtomicLong(initial.tolong()));
          }
        });
    globals.set(
        "newAtomicBool",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue initial) {
            return CoerceJavaToLua.coerce(new AtomicBoolean(initial.toboolean()));
          }
        });
    globals.set(
        "newCountDownLatch",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue count) {
            return CoerceJavaToLua.coerce(new CountDownLatch(count.checkint()));
          }
        });
    globals.set(
        "newCyclicBarrier",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue parties) {
            return CoerceJavaToLua.coerce(new CyclicBarrier(parties.checkint()));
          }
        });
    globals.set(
        "semaphore",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue permits) {
            return CoerceJavaToLua.coerce(new Semaphore(permits.checkint()));
          }
        });
    globals.set(
        "lock",
        new ZeroArgFunction() {
          @Override
          public LuaValue call() {
            return CoerceJavaToLua.coerce(new ReentrantLock());
          }
        });
  }
}
