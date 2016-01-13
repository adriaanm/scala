package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class Annotation implements java.io.Serializable
{
        public Annotation(Type base, AnnotationArgument[] arguments)
        {
                this.base = base;
                this.arguments = arguments;
        }
        private final Type base;
        private final AnnotationArgument[] arguments;
        public final Type base()
        {
                return base;
        }
        public final AnnotationArgument[] arguments()
        {
                return arguments;
        }
        public boolean equals(Object obj)
        {
                 if (this == obj) {
                         return true;
                } else if (!(obj instanceof Annotation)) {
                         return false;
                } else {
                        Annotation o = (Annotation)obj;
                        return base().equals(o.base()) && Arrays.deepEquals(arguments(), o.arguments());
                }
        }
        public int hashCode()
        {
                 return 37 * (37 * (17) + base().hashCode()) + Arrays.deepHashCode(arguments());
        }
        public String toString()
        {
                 return "Annotation(" + "base: " + base() + ", " + "arguments: " + Arrays.toString(arguments()) + ")";
        }
}
