package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class OutputSetting implements java.io.Serializable
{
        public OutputSetting(String sourceDirectory, String outputDirectory)
        {
                this.sourceDirectory = sourceDirectory;
                this.outputDirectory = outputDirectory;
        }
        private final String sourceDirectory;
        private final String outputDirectory;
        public final String sourceDirectory()
        {
                return sourceDirectory;
        }
        public final String outputDirectory()
        {
                return outputDirectory;
        }
        public boolean equals(Object obj)
        {
                 if (this == obj) {
                         return true;
                } else if (!(obj instanceof OutputSetting)) {
                         return false;
                } else {
                        OutputSetting o = (OutputSetting)obj;
                        return sourceDirectory().equals(o.sourceDirectory()) && outputDirectory().equals(o.outputDirectory());
                }
        }
        public int hashCode()
        {
                 return 37 * (37 * (17) + sourceDirectory().hashCode()) + outputDirectory().hashCode();
        }
        public String toString()
        {
                 return "OutputSetting(" + "sourceDirectory: " + sourceDirectory() + ", " + "outputDirectory: " + outputDirectory() + ")";
        }
}
