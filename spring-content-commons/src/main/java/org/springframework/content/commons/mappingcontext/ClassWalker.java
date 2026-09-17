package org.springframework.content.commons.mappingcontext;

import org.springframework.content.commons.utils.ContentPropertyUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassWalker {

    private final ClassVisitor visitor;

    public ClassWalker(ClassVisitor visitor) {
        this.visitor = visitor;
    }

    public static String propertyName(String name) {
        if (!StringUtils.hasLength(name)) {
            return name;
        }

        String propertyName = calculateName(name);
        if (propertyName != null) {
            return propertyName;
        }

        String[] segments = split(name);
        if (segments.length == 1) {
            return segments[0];
        } else {
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < segments.length - 1; i++) {
                b.append(segments[i]);
            }
            return b.toString();
        }
    }

    public static String calculateName(String name) {
        Pattern p = Pattern.compile("^(.+)(Id|Len|Length|MimeType|Mimetype|ContentType|(?<!Mime|Content)Type|(?<!Original)FileName|(?<!Original)Filename|OriginalFileName|OriginalFilename)$");
        Matcher m = p.matcher(name);
        if (!m.matches()) {
            return null;
        }
        return m.group(1);
    }

    public static String[] split(String name) {
        if (!StringUtils.hasLength(name)) {
            return new String[]{};
        }

        return name.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
    }

    public void accept(Class<?> clazz) {

        boolean fContinue = true;
        Stack<WalkContext> classStack = new Stack<>();

        fContinue &= visitor.visitClass("", clazz);
        if (!fContinue) {
            return;
        }

        List<Field> fields = getAllFields(new ArrayList<>(), clazz);

        for (Field field : fields) {
            fContinue &= visitor.visitFieldBefore("", clazz, field);
            fContinue &= visitor.visitField("", clazz, field);
            if (isObject(field)) {
                if (notContains(classStack, field.getType())) {
                    classStack.push(new WalkContext(field.getName(), field.getType()));
                    this.accept(field.getType(), classStack);
                    classStack.pop();
                }
            }
            fContinue &= visitor.visitFieldAfter("", clazz, field);
        }
        if (!fContinue) {
            return;
        }

        visitor.visitClassEnd("", clazz);
    }

    public void accept(Class<?> clazz, Stack<WalkContext> classStack) {

        boolean fContinue = true;

        WalkContext context = classStack.peek();

        fContinue &= visitor.visitClass(context.path(), clazz);
        if (!fContinue) {
            return;
        }

        List<Field> fields = getAllFields(new ArrayList<>(), context.clazz());

        for (Field field : fields) {
            fContinue &= visitor.visitFieldBefore("", clazz, field);
            fContinue &= visitor.visitField(context.path(), clazz, field);
            if (isObject(field)) {
                if (notContains(classStack, field.getType())) {
                    String path = field.getName();
                    if (StringUtils.hasLength(context.path())) {
                        path = String.format("%s/%s", context.path(), path);
                    }
                    classStack.push(new WalkContext(path, field.getType()));
                    this.accept(field.getType(), classStack);
                    classStack.pop();
                }
            }
            fContinue &= visitor.visitFieldAfter("", clazz, field);
        }
        if (!fContinue) {
            return;
        }

        visitor.visitClassEnd(context.path(), clazz);
    }

    private boolean notContains(Stack<WalkContext> classStack, Class<?> type) {

        for (WalkContext context : classStack) {
            if (context.clazz().equals(type)) {
                return false;
            }
        }
        return true;
    }

    private boolean isObject(Field field) {
        return !field.getType().isPrimitive() &&
                !field.getType().equals(String.class) &&
                !field.getType().equals(UUID.class) &&
                !field.getType().isEnum() &&
                !ContentPropertyUtils.isWrapperType(field.getType()) &&
                !ContentPropertyUtils.isRelationshipField(field);
    }

    private List<Field> getAllFields(List<Field> fields, Class<?> type) {
        fields.addAll(Arrays.asList(type.getDeclaredFields()));

        if (type.getSuperclass() != null) {
            getAllFields(fields, type.getSuperclass());
        }

        return fields;
    }

    public record WalkContext(String path, Class<?> clazz) {
    }
}
