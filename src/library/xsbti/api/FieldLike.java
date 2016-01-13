package xsbti.api;

import java.util.Arrays;
import java.util.List;
public abstract class FieldLike extends Definition
{
        public FieldLike(Type tpe, String name, Access access, Modifiers modifiers, Annotation[] annotations)
        {
                super(name, access, modifiers, annotations);
                this.tpe = tpe;
        }
        private final Type tpe;
        public final Type tpe()
        {
                return tpe;
        }
}
