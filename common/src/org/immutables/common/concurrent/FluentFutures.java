package org.immutables.common.concurrent;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ForwardingListenableFuture.SimpleForwardingListenableFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.FutureFallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Provides default wraping implementation of {@link FluentFuture}
 */
public final class FluentFutures {
  private FluentFutures() {
  }

  private static final class WrapingFluentFuture<V>
      extends SimpleForwardingListenableFuture<V>
      implements FluentFuture<V> {

    private WrapingFluentFuture(ListenableFuture<V> future) {
      super(future);
    }

    @Override
    public V getUnchecked() {
      return Futures.getUnchecked(delegate());
    }

    @Override
    public FluentFuture<V> addCallback(FutureCallback<V> callback) {
      Futures.addCallback(this, callback);
      return this;
    }

    @Override
    public FluentFuture<V> withFallback(FutureFallback<V> fallback) {
      return from(Futures.withFallback(this, fallback));
    }

    @Override
    public FluentFuture<V> withFallbackValue(final V value) {
      return withFallback(new FutureFallback<V>() {
        @Override
        public ListenableFuture<V> create(Throwable t) throws Exception {
          return Futures.immediateFuture(value);
        }
      });
    }

    @Override
    public <T> FluentFuture<T> transform(Function<? super V, ? extends T> function) {
      return from(Futures.transform(this, function));
    }

    @Override
    public <T> FluentFuture<T> transform(AsyncFunction<? super V, ? extends T> function) {
      return from(Futures.transform(this, function));
    }
  }

  /**
   * Unnecessary conversion, already have a fluent future. This method exists solely to detect
   * unnecessary conversion from a simple time.
   * @param <V> value type
   * @param future future
   * @return same instance
   */
  @Deprecated
  public static <V> FluentFuture<V> from(FluentFuture<V> future) {
    return future;
  }

  /**
   * Wraps listenable future with a fluent future.
   * @param <V> value type
   * @param future future
   * @return fluent instance
   */
  public static <V> FluentFuture<V> from(ListenableFuture<V> future) {
    if (future instanceof FluentFuture<?>) {
      return (FluentFuture<V>) future;
    }
    return new WrapingFluentFuture<>(future);
  }

  /**
   * Dereferencing only present optional value, otherwise {@link IllegalStateException} will be
   * notified as failure.
   * @param <T> the generic type
   * @param callback the callback
   * @return the future callback
   */
  // XXX Have some doubt that it pulls it conceptual weight. i.e. is barely useful
  public static <T> FutureCallback<Optional<T>> presentOnly(FutureCallback<T> callback) {
    return new OptionalDereferencingFutureCallback<>(callback);
  }

  private static final class OptionalDereferencingFutureCallback<T>
      implements FutureCallback<Optional<T>> {
    private final FutureCallback<T> callback;

    OptionalDereferencingFutureCallback(FutureCallback<T> callback) {
      this.callback = callback;
    }

    @Override
    public void onSuccess(Optional<T> result) {
      try {
        callback.onSuccess(result.get());
      } catch (IllegalStateException ex) {
        onFailure(ex);
      }
    }

    @Override
    public void onFailure(Throwable t) {
      callback.onFailure(t);
    }
  }

}
