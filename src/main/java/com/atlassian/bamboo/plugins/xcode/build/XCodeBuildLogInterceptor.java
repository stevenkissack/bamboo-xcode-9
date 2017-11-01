package com.atlassian.bamboo.plugins.xcode.build;

import com.atlassian.bamboo.build.LogEntry;
import com.atlassian.bamboo.build.logger.LogInterceptor;
import org.jetbrains.annotations.NotNull;
import org.apache.commons.lang.StringUtils;

import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;

public final class XCodeBuildLogInterceptor implements LogInterceptor
{
    private BufferedWriter bw;

    @Override
    public void intercept(@NotNull final LogEntry logEntry)
    {
        processLine(logEntry.getUnstyledLog());
    }

    @Override
    public void interceptError(@NotNull final LogEntry logEntry)
    {
    }

    private void processLine(@NotNull final String line)
    {
        if (bw == null)
        {
            return;
        }

        try
        {
            bw.write(line);
            bw.newLine();
        }
        catch (IOException e)
        {
            /* Just drop the line */
        }
    }

    public void open(final String path, final boolean append) throws IOException
    {
        if (StringUtils.isNotEmpty(path))
        {
            if (bw != null)
            {
                close();
            }

            bw = new BufferedWriter(new FileWriter(path, append));
        }
    }

    public void close()
    {
        try
        {
            if (bw != null)
            {
                bw.close();
            }
        }
        catch (IOException e)
        {
            /* Can't do much */
        }
    }
}
