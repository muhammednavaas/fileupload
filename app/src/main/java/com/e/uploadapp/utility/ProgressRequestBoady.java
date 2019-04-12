package com.e.uploadapp.utility;

import java.io.IOException;

import io.reactivex.annotations.NonNull;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

public class ProgressRequestBoady extends RequestBody {
    private final RequestBody delegate;
    private final Listener listener;

    public ProgressRequestBoady(RequestBody delegate, Listener listener) {
        this.delegate = delegate;
        this.listener = listener;
    }


    @Override
    public MediaType contentType() {
        return delegate.contentType();
    }

    @Override
    public long contentLength() throws IOException {
        try {
            return delegate.contentLength();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }


    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        CountingSink countingSink = new CountingSink(sink);
        BufferedSink bufferedSink = Okio.buffer(countingSink);
        delegate.writeTo(bufferedSink);
        bufferedSink.flush();
    }

    public interface Listener {
        void onRequestProgress(long bye, long contentLength);
    }

    private class CountingSink extends ForwardingSink {
        private long byteWritten = 0;

        CountingSink(Sink delegate) {
            super(delegate);
        }

        @Override
        public void write(@NonNull Buffer source, long byteCount) throws IOException {
            super.write(source, byteCount);
            byteWritten += byteCount;
            listener.onRequestProgress(byteWritten, contentLength());
        }
    }
}
