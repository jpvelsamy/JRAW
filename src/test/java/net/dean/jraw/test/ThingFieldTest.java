package net.dean.jraw.test;

import junit.framework.Assert;
import net.dean.jraw.NetworkException;
import net.dean.jraw.RedditClient;
import net.dean.jraw.models.*;
import net.dean.jraw.models.core.Account;
import net.dean.jraw.models.core.Listing;
import net.dean.jraw.models.core.Submission;
import net.dean.jraw.pagination.SimplePaginator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ThingFieldTest {
	private static final String SUBMISSION_ID = "92dd8";
	private static RedditClient reddit;

	@BeforeClass
	public static void setUp() {
		reddit = TestUtils.client(ThingFieldTest.class);
	}

	static <T extends JsonModel> void fieldValidityCheck(T thing) {
		List<Method> jsonInteractionMethods = getJsonInteractionMethods(thing.getClass());

		try {
			for (Method method : jsonInteractionMethods) {
				JsonInteraction jsonInteraction = method.getAnnotation(JsonInteraction.class);
				try {
					method.invoke(thing);
				} catch (InvocationTargetException e) {
					// InvocationTargetException thrown when the method.invoke() returns null and @JsonInteraction "nullable"
					// property is false
					if (e.getCause().getClass().equals(NullPointerException.class) && !jsonInteraction.nullable()) {
						Assert.fail("Non-nullable JsonInteraction method returned null: " + thing.getClass().getName() + "." + method.getName() + "()");
					} else {
						// Other reason for InvocationTargetException
						Assert.fail(e.getCause().getMessage());
					}
				}
			}
		} catch (IllegalAccessException e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Gets a list of fields that have the AttributeField annotation attached to them. Also searches the superclass up
	 * until ${@link net.dean.jraw.models.core.Thing} for fields.
	 *
	 * @param thingClass The class to search for
	 * @return A list of fields that have the JsonAttribute annotation
	 */
	private static List<Method> getJsonInteractionMethods(Class<? extends JsonModel> thingClass) {
		List<Method> methods = new ArrayList<>();

		Class clazz = thingClass;
		List<Method> toObserve = new ArrayList<>();

		while (clazz != null) {
			toObserve.addAll(Arrays.asList(clazz.getDeclaredMethods()));
			for (Class<?> interf : clazz.getInterfaces()) {
				toObserve.addAll(Arrays.asList(interf.getDeclaredMethods()));
			}

			if (clazz.equals(RedditObject.class)) {
				// Already at the highest level and we don't need to scan Object
				break;
			}

			// Can still go deeper...
			clazz = clazz.getSuperclass();
		}

		methods.addAll(toObserve.stream().filter(m -> m.isAnnotationPresent(JsonInteraction.class)).collect(Collectors.toList()));

		return methods;
	}

	@Test
	public void testAccount() {
		try {
			Account redditAccount = reddit.getUser("spladug");
			fieldValidityCheck(redditAccount);
		} catch (NetworkException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testLink() {
		try {
			Submission submission = reddit.getSubmission(SUBMISSION_ID);
			fieldValidityCheck(submission);
		} catch (NetworkException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test(dependsOnMethods = "testLink")
	public void testComment() {
		try {
			Submission submission = reddit.getSubmission(SUBMISSION_ID);
			fieldValidityCheck(submission.getComments().getChildren().get(0));
		} catch (NetworkException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testOEmbed() {
		try {
			SimplePaginator frontPage = reddit.getFrontPage();
			Listing<Submission> submissions = frontPage.next();

			submissions.getChildren().stream().filter(s -> s.getOEmbedMedia() != null).forEach(s -> {
				OEmbed o = s.getOEmbedMedia();
				fieldValidityCheck(o);
			});
		} catch (NetworkException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testEmbeddedMedia() {
		try {
			SimplePaginator frontPage = reddit.getFrontPage();
			Listing<Submission> submissions = frontPage.next();

			submissions.getChildren().stream().filter(s -> s.getEmbeddedMedia() != null).forEach(s -> {
				EmbeddedMedia m = s.getEmbeddedMedia();
				fieldValidityCheck(m);
			});
		} catch (NetworkException e) {
			Assert.fail(e.getMessage());
		}
	}
}
