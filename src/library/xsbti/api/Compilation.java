package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class Compilation implements java.io.Serializable
{
        public Compilation(long startTime, OutputSetting[] outputs)
        {
                this.startTime = startTime;
                this.outputs = outputs;
        }
        private final long startTime;
        private final OutputSetting[] outputs;
        public final long startTime()
        {
                return startTime;
        }
        public final OutputSetting[] outputs()
        {
                return outputs;
        }
        public boolean equals(Object obj)
        {
                 if (this == obj) {
                         return true;
                } else if (!(obj instanceof Compilation)) {
                         return false;
                } else {
                        Compilation o = (Compilation)obj;
                        return startTime() == o.startTime() && Arrays.deepEquals(outputs(), o.outputs());
                }
        }
        public int hashCode()
        {
                 return 37 * (37 * (17) + (int)(startTime() ^ (startTime() >>> 32))) + Arrays.deepHashCode(outputs());
        }
        public String toString()
        {
                 return "Compilation(" + "startTime: " + startTime() + ", " + "outputs: " + Arrays.toString(outputs()) + ")";
        }
}
