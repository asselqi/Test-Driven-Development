package Solution;

import Provided.*;
import org.junit.ComparisonFailure;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;


public class StoryTesterImpl implements StoryTester {
    private StoryTestExceptionImpl story_exception;
    private static boolean when_sequence = true;

    public StoryTesterImpl() {
        story_exception = new StoryTestExceptionImpl();
    }

    private static boolean annotationMatch(String annotationString, String[] sentence, ArrayList<Object> params) {
        String[] annotation_arr = annotationString.split(" ");
        if (annotation_arr.length != sentence.length)
            return false;
        for (int i = 0; i < sentence.length; i++) {
            if (annotation_arr[i].matches("&[a-zA-Z]*")) {
                if (sentence[i].matches("-?[0-9]+")) {
                    Integer int_param = Integer.parseInt(sentence[i]);
                    params.add(int_param);
                } else {
                    params.add(sentence[i]);
                }
            } else if (!annotation_arr[i].equals(sentence[i])) {
                return false;
            }
        }
        return true;
    }

    private static void testOnInheritanceTreeWhenGiven(String[] sentence, Class<?> testClass, Field[] class_fields,
                                                       Map<String, Object> backup, Object instance) throws WordNotFoundException,
            InvocationTargetException, IllegalAccessException {
        if (testClass == null) {
            if (sentence[0].equals("Given"))
                throw new GivenNotFoundException();
            else if (sentence[0].equals("When"))
                throw new WhenNotFoundException();
        }
        boolean found = false;
        Method[] my_methods = testClass.getDeclaredMethods();
        for (Method m : my_methods) {
            ArrayList<Object> params = new ArrayList<Object>();
            if (sentence[0].equals("Given")) {
                if (m.isAnnotationPresent(Given.class)) {
                    Given annotation = m.getAnnotation(Given.class);
                    String[] sentence_arr = new String[sentence.length - 1];
                    System.arraycopy(sentence, 1, sentence_arr, 0, sentence.length - 1);
                    if (annotationMatch(annotation.value(), sentence_arr, params)) {
                        try {
                            m.setAccessible(true);
                            m.invoke(instance, params.toArray());
                        } catch (Exception e) {
                            throw e;
                        }
                        found = true;
                    }
                }
            } else if (sentence[0].equals("When")) {
                if (m.isAnnotationPresent(When.class)) {
                    When annotation = m.getAnnotation(When.class);
                    String[] sentence_arr = new String[sentence.length - 1];
                    System.arraycopy(sentence, 1, sentence_arr, 0, sentence.length - 1);
                    if (annotationMatch(annotation.value(), sentence_arr, params)) {
                        try {
                            if (when_sequence)
                                backupClassFields(class_fields, backup, instance);
                            m.setAccessible(true);
                            m.invoke(instance, params.toArray());
                            when_sequence = false;
                        } catch (Exception e) {
                            throw e;
                        }
                        found = true;
                    }
                }
            }
        }
        if (!found) {
            try {
                testOnInheritanceTreeWhenGiven(sentence, testClass.getSuperclass(), class_fields, backup, instance);
            } catch (WordNotFoundException e) {
                throw e;
            }
        }
    }

    private static void testOnInheritanceTreeThen(String then_str, Class<?> testClass, Object instance)
            throws ComparisonFailure, ThenNotFoundException, InvocationTargetException, IllegalAccessException {
        if (testClass == null)
            throw new ThenNotFoundException();
        boolean found = false;
        Method[] my_methods = testClass.getDeclaredMethods();
        for (Method m : my_methods) {
            ArrayList<Object> params = new ArrayList<Object>();
            if (m.isAnnotationPresent(Then.class)) {
                Then annotation = m.getAnnotation(Then.class);
                if (annotationMatch(annotation.value(), then_str.split(" "), params)) {
                    m.setAccessible(true);
                    try {
                        m.invoke(instance, params.toArray());
                    } catch (Exception e) {
                        throw e;
                    }
                    found = true;
                }
            }
        }
        if (!found) {
            try {
                testOnInheritanceTreeThen(then_str, testClass.getSuperclass(), instance);
            } catch (ComparisonFailure e) {
                throw e;
            } catch (WordNotFoundException e) {
                throw e;
            }
        }
    }

    @Override
    public void testOnInheritanceTree(String story, Class<?> testClass) throws Exception {
        //TODO: Implement
        if (testClass == null || story == null)
            throw new IllegalArgumentException();
        story_exception.setFailed_then("");
        int then_counter = 0, then_success = 0, sub_sentence_failure; //counter to how many then sentences
        //counter to how many then sentences succeeded
        // counter to how many sub-sentences of the same then sentence failed, to check if the sentence failed;
        boolean first_then_failure = false;
        StorySpliter sp = new StorySpliter("\n");
        String[] sentences = sp.splitFunction(story);
        Map<String, Object> backup = new HashMap<>();
        List<String> temp_expected = new ArrayList<String>();
        List<String> temp_actual = new ArrayList<String>();
        Constructor<?> constructor;
        Object instance;
        Class<?> c = testClass;
        ArrayList<Class<?>> enclosing_classes = new ArrayList<Class<?>>();
        ArrayList<Class<?>> prev_enclosing_classes = new ArrayList<Class<?>>();
        if (testClass.getEnclosingClass() != null) {
            while (c.getEnclosingClass() != null) {
                enclosing_classes.add(c.getEnclosingClass());
                c = c.getEnclosingClass();
            }
            //enclosing_classes.add(c.getEnclosingClass());
            Object enclosing_instance = enclosing_classes.get(enclosing_classes.size() - 1).newInstance();
            prev_enclosing_classes.add(enclosing_classes.get(enclosing_classes.size() - 1));
            enclosing_classes.remove(enclosing_classes.size() - 1);
            while (enclosing_classes.size() > 0) {
                Constructor<?> ctor = enclosing_classes.get(enclosing_classes.size() - 1).
                        getDeclaredConstructor(prev_enclosing_classes.get(enclosing_classes.size() - 1));
                Object inner = ctor.newInstance(enclosing_instance);
                enclosing_instance = inner;
                prev_enclosing_classes.add(enclosing_classes.get(enclosing_classes.size() - 1));
                enclosing_classes.remove(enclosing_classes.size() - 1);

            }
            Constructor<?> ctor = testClass.getDeclaredConstructor(testClass.getEnclosingClass());
            instance = ctor.newInstance(enclosing_instance);
        } else {
            constructor = testClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            instance = constructor.newInstance();
        }
        Field[] class_fields = instance.getClass().getDeclaredFields();
        for (String sentence : sentences) {
            temp_expected.clear();
            temp_actual.clear();
            if (!sentence.split(" ")[0].equals("Then")) {
                try {
                    testOnInheritanceTreeWhenGiven(sentence.split(" "), testClass, class_fields, backup, instance);
                } catch (WordNotFoundException e) {
                    throw e;
                }
            } else {
                sub_sentence_failure = 0;
                then_counter++;
                when_sequence = true;
                String[] Then_sentence = sentence.split("Then ")[1].split(" or ");
                for (String then_str : Then_sentence) {
                    try {
                        testOnInheritanceTreeThen(then_str, testClass, instance);
                        then_success++;
                        break;
                    } catch (InvocationTargetException assertFailedInvo) {
                        ComparisonFailure e = new ComparisonFailure("", "", "");
                        if (assertFailedInvo.getTargetException().getClass() == e.getClass()) {
                            ComparisonFailure failure = (ComparisonFailure) assertFailedInvo.getTargetException();
                            sub_sentence_failure++;
                            if (!first_then_failure) {
                                temp_expected.add(failure.getExpected());
                                temp_actual.add(failure.getActual());
                            }
                            if (sub_sentence_failure == Then_sentence.length && !first_then_failure) {
                                story_exception.setFailed_then(sentence);
                                story_exception.setExpected(temp_expected);
                                story_exception.setActual(temp_actual);
                            }
                            if (sub_sentence_failure == Then_sentence.length)
                                restoreClassField(class_fields, backup, instance);
                        }

                    }
                }
                if (!story_exception.getSentance().isEmpty())
                    first_then_failure = true;
            }
        }
        story_exception.setFailNum(then_counter - then_success);
        if (then_counter > then_success)
            throw story_exception;
    }

    private static boolean getGivenClass(Class<?> testClass, String given_value) {
        if (testClass == null)
            return false;
        boolean is_given = (Arrays.stream(testClass.getDeclaredMethods()).
                filter(m -> m.isAnnotationPresent(Given.class)).
                filter(m -> m.getAnnotation(Given.class).value().
                        matches(given_value + " " + "&[A-Za-z]*")).
                collect(Collectors.toSet()).size() > 0);
        if (is_given)
            return true;
        return getGivenClass(testClass.getSuperclass(), given_value);
    }

    private static void getFirstClass(Class<?> testClass, ArrayList<Class<?>> result, String given_vlaue)
            throws GivenNotFoundException {
        if (getGivenClass(testClass, given_vlaue)) {
            result.add(testClass);
            return;
        }
        Class<?>[] my_nested = testClass.getDeclaredClasses();

        for (Class<?> nested : my_nested) {
            getFirstClass(nested, result, given_vlaue);
            if (!result.isEmpty())
                break;
        }
        if (result == null)
            throw new GivenNotFoundException();
    }

    @Override
    public void testOnNestedClasses(String story, Class<?> testClass) throws Exception {
        //TODO: Implement
        if (testClass == null || story == null)
            throw new IllegalArgumentException();
        StorySpliter sp = new StorySpliter("\n");
        String[] sentences = sp.splitFunction(story);
        String[] sentence = sentences[0].split("Given ")[1].split(" ");
        String[] sentence_arr = new String[sentence.length - 1];
        System.arraycopy(sentence, 0, sentence_arr, 0, sentence.length - 1);
        String given_value = String.join(" ", sentence_arr);
        ArrayList<Class<?>> result = new ArrayList<Class<?>>();
        getFirstClass(testClass, result, given_value);
        if (result.isEmpty())
            throw new GivenNotFoundException();
        testOnInheritanceTree(story, result.get(0));
    }

    public static void backupClassFields(Field[] class_fields, Map<String, Object> backup, Object instance) {
        try {
            boolean backedup;
            for (Field f : class_fields) {
                backedup = false;
                f.setAccessible(true);
                // First case, check if the field object implements cloneable
                List<Class<?>> interfaces = Arrays.stream(f.getType().getInterfaces()).collect(Collectors.toList());
                if (f.get(instance) == null) {
                    backup.put(f.toString(), null);
                } else if (f.get(instance) instanceof Cloneable) {
                    Class<?> iter_class = f.getType();
                    while (iter_class != null) {
                        Method[] methods = iter_class.getDeclaredMethods();
                        for (Method m : methods) {
                            if (m.getName().equals("clone") && m.getParameterCount() == 0) {
                                m.setAccessible(true);
                                Cloneable value = (Cloneable) f.get(instance);
                                Object field_clone = m.invoke(value);
                                backup.put(f.toString(), field_clone);
                                backedup = true;
                                break;
                            }
                        }
                        if (backedup) break;
                        iter_class = iter_class.getSuperclass();
                    }
                } else {
                    //Second case, check if the object has a copy constructor
                    Constructor<?>[] constructors = f.getType().getDeclaredConstructors();
                    for (Constructor<?> c : constructors) {
                        if (backedup) break;
                        Class<?>[] typeVariables = c.getParameterTypes();
                        for (Class<?> t : typeVariables) {
                            if (t.equals(f.getType()) && typeVariables.length == 1) {
                                c.setAccessible(true);
                                backup.put(f.toString(), c.newInstance(f.get(instance)));
                                backedup = true;
                                break;
                            }
                        }
                    }
                    //Third case, if not backedup yet, save the object itself
                    if (!backedup)
                        backup.put(f.toString(), f.get(instance));
                }
            }
        } catch (Exception e) {
        }

    }

    public static void restoreClassField(Field[] class_fields, Map<String, Object> backup, Object instance)
            throws IllegalAccessException {
        for (Field f : class_fields) {
            f.setAccessible(true);
            f.set(instance, backup.get(f.toString()));
        }
    }
}
