//Parameters
const userName = "%1$s"
const cssInject = "%2$s"

function waitForElement(document, selector) {
	return new Promise((resolve) => {
		if(document.querySelector(selector)) {
			return resolve(document.querySelector(selector))
		}
		
		const observer = new MutationObserver((mutations) => {
			if(document.querySelector(selector)) {
				resolve(document.querySelector(selector))
				observer.disconnect()
			}
		})
		
		observer.observe(document.body, {
			childList: true,
			subtree: true
		})
	})
}

function waitForIframe(element) {
	return new Promise((resolve) => {
		element.addEventListener("load", resolve)
	})
}

(async () => {
	//Wait for the FaceTime iframe to load
	const facetime = await waitForElement(document, "#facetime")
	await waitForIframe(facetime)
	const facetimeDocument = facetime.contentWindow.document
	
	{
		//Inject CSS
		const elementHead = facetimeDocument.getElementsByTagName("head")[0]
		
		const elementStyle = facetimeDocument.createElement("style")
		elementStyle.textContent = window.atob(cssInject)
		elementHead.appendChild(elementStyle)
	}
	
	{
		//Get the input
		const inputName = await waitForElement(facetimeDocument, "#name-entry")
		
		//Create an event for React to recognize
		const nativeInputValueSetter = Object.getOwnPropertyDescriptor(
			window.HTMLInputElement.prototype,
			"value"
		).set
		nativeInputValueSetter.call(inputName, userName)
		
		//Update name
		const inputEvent = new Event("input", {bubbles: true})
		inputName.dispatchEvent(inputEvent)
		
		//Enter
		const returnEvent = new KeyboardEvent("keydown", {
			bubbles: true, cancelable: true, keyCode: 13
		})
		inputName.dispatchEvent(returnEvent)
	}
	
	{
		//Click the join button
		const buttonJoin = await waitForElement(facetimeDocument, "#callcontrols-join-button-session-banner")
		buttonJoin.click()
	}
})()