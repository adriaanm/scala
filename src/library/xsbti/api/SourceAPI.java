package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class SourceAPI implements java.io.Serializable
{
        public SourceAPI(Package[] packages, Definition[] definitions)
        {
                this.packages = packages;
                this.definitions = definitions;
        }
        private final Package[] packages;
        private final Definition[] definitions;
        public final Package[] packages()
        {
                return packages;
        }
        public final Definition[] definitions()
        {
                return definitions;
        }
        public boolean equals(Object obj)
        {
                 if (this == obj) {
                         return true;
                } else if (!(obj instanceof SourceAPI)) {
                         return false;
                } else {
                        SourceAPI o = (SourceAPI)obj;
                        return Arrays.deepEquals(packages(), o.packages()) && Arrays.deepEquals(definitions(), o.definitions());
                }
        }
        public int hashCode()
        {
                 return 37 * (37 * (17) + Arrays.deepHashCode(packages())) + Arrays.deepHashCode(definitions());
        }
        public String toString()
        {
                 return "SourceAPI(" + "packages: " + Arrays.toString(packages()) + ", " + "definitions: " + Arrays.toString(definitions()) + ")";
        }
}
