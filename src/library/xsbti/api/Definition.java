package xsbti.api;

import java.util.Arrays;
import java.util.List;
public abstract class Definition implements java.io.Serializable
{
        public Definition(String name, Access access, Modifiers modifiers, Annotation[] annotations)
        {
                this.name = name;
                this.access = access;
                this.modifiers = modifiers;
                this.annotations = annotations;
        }
        private final String name;
        private final Access access;
        private final Modifiers modifiers;
        private final Annotation[] annotations;
        public final String name()
        {
                return name;
        }
        public final Access access()
        {
                return access;
        }
        public final Modifiers modifiers()
        {
                return modifiers;
        }
        public final Annotation[] annotations()
        {
                return annotations;
        }
}
