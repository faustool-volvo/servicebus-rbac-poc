# About

The object of this program is to show how Azure Service Bus can be configured to authenticate consumers using RBAC and Azure AD in addition to SAS tokens.

# Prerequisites

* Apache Maven 3.8
* Java JDK 11
* Azure CLI

# Building

To build and test, just execute maven:
```
mvn clean install
```

# Preparing the Azure Environment

Login to Azure:
```powershell
az login
```

Set the default subscription:
```powershell
az account set --subscription "{your subscription id}"
```

Prepare an object to store the items created:
```powershell
$Poc = @{}
$Poc.AzureSubscription = az account show | ConvertFrom-Json
```

> As you progress through this guide, it is recommended to execute the `.\SavePoc.ps1` script after each command to save your progress into the `.pocdata` file.  
> To resume work, execute the command `$Poc = .\ReadPoc.ps1`

Confirm you have the correct subscription:
```powershell
az account show
```

Create the execution group for the POC:
```powershell
$Poc.Group = az group create `
    --name "ServiceBusPOC" `
    --location "eastus2" | ConvertFrom-Json
```
> You can use whatever name and location you want because `ConvertFrom-Json` will parse the output of the `az` command and store it in a variable.

Create the Service Bus Namespace:
```powershell
$Poc.Namespace = az servicebus namespace create `
    --name "Contracts-$env:USERNAME" `
    --sku standard `
    --location $Poc.Group.Location `
    --resource-group $Poc.Group.Name | ConvertFrom-Json
```

Create the topics:
```powershell
$Poc.TopicA = az servicebus topic create `
    --name "ContractsA" `
    --namespace-name $Poc.Namespace.Name `
    --resource-group $Poc.Group.Name | ConvertFrom-Json
```
```powershell
$Poc.TopicB = az servicebus topic create `
    --name "ContractsB" `
    --namespace-name $Poc.Namespace.Name `
    --resource-group $Poc.Group.Name | ConvertFrom-Json
```

Create the subscriptions:
```powershell
$Poc.SubscriptionA1 = az servicebus topic subscription create `
    --name "ConsumerA1" `
    --topic-name $Poc.TopicA.Name `
    --namespace-name $Poc.Namespace.Name `
    --resource-group $Poc.Group.Name `
    --max-delivery-count 1 | ConvertFrom-Json
```
```powershell
$Poc.SubscriptionA2 = az servicebus topic subscription create `
    --name "ConsumerA2" `
    --topic-name $Poc.TopicA.Name `
    --namespace-name $Poc.Namespace.Name `
    --resource-group $Poc.Group.Name `
    --max-delivery-count 1 | ConvertFrom-Json
```
```powershell
$Poc.SubscriptionB1 = az servicebus topic subscription create `
    --name "ConsumerB1" `
    --topic-name $Poc.TopicB.Name `
    --namespace-name $Poc.Namespace.Name `
    --resource-group $Poc.Group.Name `
    --max-delivery-count 1 | ConvertFrom-Json
```
```powershell
$Poc.SubscriptionB2 = az servicebus topic subscription create `
    --name "ConsumerB2" `
    --topic-name $Poc.TopicB.Name `
    --namespace-name $Poc.Namespace.Name `
    --resource-group $Poc.Group.Name `
    --max-delivery-count 1 | ConvertFrom-Json
```

Create the app registrations and service principals:
```powershell
# Control user (no access)
$Poc.ControlApp = az ad app create `
    --display-name "SBUS-RBAC-POC-$env:USERNAME-Control" `
    --available-to-other-tenants false | ConvertFrom-Json
$Poc.ControlPrincipal = az ad sp create-for-rbac `
    --name $Poc.ControlApp.DisplayName `
    --role Reader `
    --scopes $Poc.Group.Id | ConvertFrom-Json
```
```powershell
# Publisher
$Poc.PublisherApp = az ad app create `
    --display-name "SBUS-RBAC-POC-$env:USERNAME-Publisher" `
    --available-to-other-tenants false | ConvertFrom-Json
$Poc.PublisherPrincipal = az ad sp create-for-rbac `
    --name $Poc.PublisherApp.DisplayName `
    --role Reader `
    --scopes $Poc.Group.Id | ConvertFrom-Json
```
```powershell
# Consumer A1
$Poc.ConsumerA1App = az ad app create `
    --display-name "SBUS-RBAC-POC-$env:USERNAME-Consumer-A1" `
    --available-to-other-tenants false | ConvertFrom-Json
$Poc.ConsumerA1Principal = az ad sp create-for-rbac `
    --name $Poc.ConsumerA1App.DisplayName `
    --role Reader `
    --scopes $Poc.Group.Id | ConvertFrom-Json
```
```powershell
# Consumer A2
$Poc.ConsumerA2App = az ad app create `
    --display-name "SBUS-RBAC-POC-$env:USERNAME-Consumer-A2" `
    --available-to-other-tenants false | ConvertFrom-Json
$Poc.ConsumerA2Principal = az ad sp create-for-rbac `
    --name $Poc.ConsumerA2App.DisplayName `
    --role Reader `
    --scopes $Poc.Group.Id | ConvertFrom-Json
```
```powershell
# Consumer B1
$Poc.ConsumerB1App = az ad app create `
    --display-name "SBUS-RBAC-POC-$env:USERNAME-Consumer-B1" `
    --available-to-other-tenants false | ConvertFrom-Json
$Poc.ConsumerB1Principal = az ad sp create-for-rbac `
    --name $Poc.ConsumerB1App.DisplayName `
    --role Reader `
    --scopes $Poc.Group.Id | ConvertFrom-Json
```
```powershell
# Consumer B2
$Poc.ConsumerB2Identity = az identity create `
    --name "Consumer-B2" `
    --resource-group $Poc.Group.Name | ConvertFrom-Json
```

Assign roles:
> Role names are fixed and documented at [Service Bus Data Sender](https://docs.microsoft.com/en-us/azure/role-based-access-control/built-in-roles#azure-service-bus-data-sender) and [Service Bus Data Receiver](https://docs.microsoft.com/en-us/azure/role-based-access-control/built-in-roles#azure-service-bus-data-receiver)
```powershell
$DataSenderRole="69a216fc-b8fb-44d8-bc22-1f3c2cd27a39"
$DataReceiverRole="4f6d3b9b-027b-4f4c-9142-0e5a2a2247e0"
```
```powershell
# Publisher can send to TopicA and TopicB
az role assignment create `
    --assignee $Poc.PublisherPrincipal.AppId `
    --role $DataSenderRole `
    --scope $Poc.TopicA.Id
az role assignment create `
    --assignee $Poc.PublisherPrincipal.AppId `
    --role $DataSenderRole `
    --scope $Poc.TopicB.Id
```
```powershell
# ConsumerA1 can receive from SubscriptionA1
az role assignment create `
    --assignee $Poc.ConsumerA1Principal.AppId `
    --role $DataReceiverRole `
    --scope $Poc.SubscriptionA1.Id
```
```powershell
# ConsumerA2 can receive from SubscriptionA2
az role assignment create `
    --assignee $Poc.ConsumerA2Principal.AppId `
    --role $DataReceiverRole `
    --scope $Poc.SubscriptionA2.Id
```
```powershell
# ConsumerB1 can receive from SubscriptionB1
az role assignment create `
    --assignee $Poc.ConsumerB1Principal.AppId `
    --role $DataReceiverRole `
    --scope $Poc.SubscriptionB1.Id
```
```powershell
# ConsumerB2 can receive from SubscriptionB2
az role assignment create `
    --assignee $Poc.ConsumerB2Identity.PrincipalId `
    --role $DataReceiverRole `
    --scope $Poc.SubscriptionB2.Id
```

# Running

This program can be executed locally using:

```
mvn quarkus:dev
```

Or it can be deployed to Microsoft Azure as a Function:
```powershell
# Build and deploy using Maven
mvn clean install azure-functions:deploy `
    -DfunctionAppName="sbusrbacpoc-$env:USERNAME" `
    -DfunctionAppRegion="$($Poc.Group.Location)" `
    -DfunctionResourceGroup="$($Poc.Group.Name)" `
    -DfunctionSubscription="$($Poc.AzureSubscription.Id)"

# Assign the ConsumerB2 Managed Identity to the function so we can use it
az webapp identity assign --resource-group $Poc.Group.Name --name "sbusrbacpoc-$env:USERNAME" --identities $Poc.ConsumerB2Identity.Id
```

Test your deployment with a call to the hello endpoint:
```powershell
Invoke-WebRequest "https://sbusrbacpoc-$env:USERNAME.azurewebsites.net/api/hello"
```

# Testing


## About the function

Notes for the examples:
* `{baseUrl}` is the HTTP location of your service, e.g. `http://localhost:8080` if you're running locally or `https://{functionAppName}.azurewebsites.net` if you're running as an Azure Function.  
* To find the tenant id, run the command: `(az account show | ConvertFrom-Json).TenantId`  
* For code simplicity, all responses will return HTTP Status `200`. Therefore, the reponses will always contain a `success` boolean field indicating success (`true`) or failure (`false`) and a `messages` field array with additional information.
* The function also supports HTTP GET calls with the request json fields as query parameters (e.g. `{baseUrl}/api/send?clientId=value&clientSecret=value...`), but for readability the examples will focus on HTTP POST calls only.
* The Principal objects stored inside the $Poc variable contain the values for `clientId` and `clientSecret` as `AppId` and `Password`, respectively (e.g. `$Poc.PublisherPrincipal.AppId`). For the managed identity `ConsumerB2Identity`, the value of `clientId` is `ClientId`.
* Managed Identities can only be used inside Azure, so ConsumerB2 can only be tested with an Azure Function
* Replace the placeholder values in the examples with the proper contents.

### Sending messages

The `send` endpoint will send one message to the specified topic with a given content. However, the function will use the [JavaFaker](https://github.com/DiUS/java-faker) library to send messages with random [Chuck Norris Facts](https://en.wikipedia.org/wiki/Chuck_Norris_facts) if the `message` field is missing. Have fun 😄.

To send a message, make an HTTP POST call to `{baseUrl}/api/send` with the following contents:
```json
{
    "message": "message contents",
    "topicName": "ContractsA or ContractsB",
    "clientId": "publisher appId",
    "clientSecret": "publisher password",
    "tenantId": "azure tenant id"
}
```


The response will echo the body of the message sent.

### Receiving messages

The `receive` endpoint will read all messages available for a given subscription and the `messages` field in the response will contain their contents, in the order they were received.

To receive messages using Service Principals, make an HTTP POST call to `{baseUrl}/api/receive` with the following contents:
```json
{
    "topicName": "ContractsA or ContractsB",
    "subscription": "SubscriptionA1, SubscriptionA2, SubscriptionB1 or SubscriptionB2",
    "clientId": "consumer appId",
    "clientSecret": "consumer password",
    "tenantId": "azure tenant id"
}
```
To receive messages using Managed Identities, make an HTTP POST call to `{baseUrl}/api/receive` with the following contents:
```json
{
    "topicName": "ContractsA or ContractsB",
    "subscription": "SubscriptionA1, SubscriptionA2, SubscriptionB1 or SubscriptionB2",
    "accountType": "ManagedIdentity",
    "clientId": "publisher appId"
}
```

## Conducting the POC

### Prerequisites

* [Prepare the Azure Environment](#preparing-the-azure-environment).
* [Run the function](#running) locally or as an azure function.

The POC is conducted using Microsoft PowerShell and the instructions below assume you have the `$Poc` variable available with all the data from the creation steps. The same instructions can be executed using [Postman](https://www.postman.com/), `curl` or any other http client, but you will have to replace the values yourself.

Save the base url for your function in a `$FunctionBaseUrl` variable, for instance when running locally use:
```powershell
$FunctionBaseUrl = "http://localhost:8080"
```
When running in a function, use:
```powershell
$FunctionBaseUrl = "https://sbusrbacpoc-$env:USERNAME.azurewebsites.net"
```

### Control test

Run a control test to make sure you get an authorization error with the control user:
```powershell
$Body = @{
    topicName = $Poc.TopicA.Name
    namespace = $Poc.Namespace.Name + ".servicebus.windows.net"
    clientId = $Poc.ControlPrincipal.AppId
    clientSecret = $Poc.ControlPrincipal.Password
    tenantId = $Poc.AzureSubscription.TenantId
}

Invoke-WebRequest -Method POST -Uri "$FunctionBaseUrl/api/send" -Body ($Body | ConvertTo-Json)
```

If you see the `success: false` and `com.azure.messaging.servicebus.ServiceBusException: Unauthorized access` in the response contents, then the control test is successful. Anything else would indicate a problem on the POC setup and needs to be investigated.

### Publisher can send events, but no one else can

Run this command:
```powershell
$Body = @{
    topicName = $Poc.TopicA.Name
    namespace = $Poc.Namespace.Name + ".servicebus.windows.net"
    clientId = $Poc.PublisherPrincipal.AppId
    clientSecret = $Poc.PublisherPrincipal.Password
    tenantId = $Poc.AzureSubscription.TenantId
}

Invoke-WebRequest -Method POST -Uri "$FunctionBaseUrl/api/send" -Body ($Body | ConvertTo-Json)
```
Expected result: `success: true` in the response and a single Chuck Norris fact in the `messages` array.

Repeat the test with TopicB and the result will be the same:
```powershell
$Body.topicName = $Poc.TopicB.Name

Invoke-WebRequest -Method POST -Uri "$FunctionBaseUrl/api/send" -Body ($Body | ConvertTo-Json)
```

Now try changing the `clientId` and `clientSecret` to other principals and you will get the same `Unauthorized access` from the control tests.

```powershell
$Body.clientId = $Poc.ConsumerA1Principal.AppId
$Body.clientSecret = $Poc.ConsumerA1Principal.Password

Invoke-WebRequest -Method POST -Uri "$FunctionBaseUrl/api/send" -Body ($Body | ConvertTo-Json)
```
```powershell
$Body.clientId = $Poc.ConsumerA2Principal.AppId
$Body.clientSecret = $Poc.ConsumerA2Principal.Password

Invoke-WebRequest -Method POST -Uri "$FunctionBaseUrl/api/send" -Body ($Body | ConvertTo-Json)
```
```powershell
$Body.clientId = $Poc.ConsumerB1Principal.AppId
$Body.clientSecret = $Poc.ConsumerB1Principal.Password

Invoke-WebRequest -Method POST -Uri "$FunctionBaseUrl/api/send" -Body ($Body | ConvertTo-Json)
```
```powershell
# Only works with the POC running as an Azure Function
$Body.clientId = $Poc.ConsumerB2Identity.ClientId
$Body.remove("clientSecret")
$Body.accountType = "ManagedIdentity"

Invoke-WebRequest -Method POST -Uri "$FunctionBaseUrl/api/send" -Body ($Body | ConvertTo-Json)
```
**TODO:** Tests using Managed Identity ConsumerB2 will be done later.

### Consumers can read their subscriptions

Let's start receiving ConsumerA1's messages:
```powershell
$Body = @{
    topicName = $Poc.TopicA.Name
    subscription = $Poc.SubscriptionA1.Name
    namespace = $Poc.Namespace.Name + ".servicebus.windows.net"
    clientId = $Poc.ConsumerA1Principal.AppId
    clientSecret = $Poc.ConsumerA1Principal.Password
    tenantId = $Poc.AzureSubscription.TenantId
}

Invoke-WebRequest -Method POST -Uri "$FunctionBaseUrl/api/receive" -Body ($Body | ConvertTo-Json)
```
Expected response: `success: true` and the `messages` array containing all messages published from previous tests. If there are no messages available, then the array will be simply empty.

Repeat the test with other consumers and their own subscriptions and the result will be the same:
```powershell
$Body.topicName = $Poc.TopicA.Name
$Body.subscription = $Poc.SubscriptionA2.Name
$Body.clientId = $Poc.ConsumerA2Principal.AppId
$Body.clientSecret = $Poc.ConsumerA2Principal.Password

Invoke-WebRequest -Method POST -Uri "$FunctionBaseUrl/api/receive" -Body ($Body | ConvertTo-Json)
```
```powershell
$Body.topicName = $Poc.TopicB.Name
$Body.subscription = $Poc.SubscriptionB1.Name
$Body.clientId = $Poc.ConsumerB1Principal.AppId
$Body.clientSecret = $Poc.ConsumerB1Principal.Password

Invoke-WebRequest -Method POST -Uri "$FunctionBaseUrl/api/receive" -Body ($Body | ConvertTo-Json)
```
```powershell
# Only works with the POC running as an Azure Function
$Body.topicName = $Poc.TopicB.Name
$Body.subscription = $Poc.SubscriptionB2.Name
$Body.clientId = $Poc.ConsumerB2Identity.ClientId
$Body.accountType = "ManagedIdentity"
$Body.remove("clientSecret")

Invoke-WebRequest -Method POST -Uri "$FunctionBaseUrl/api/receive" -Body ($Body | ConvertTo-Json)
```

### Consumers cannot read subscriptions of others

Let's try to consume ConsumerA1's messages using ConsumerA2's credentials:
```powershell
$Body = @{
    topicName = $Poc.TopicA.Name # Topic A
    subscription = $Poc.SubscriptionA1.Name # Subscription A1
    namespace = $Poc.Namespace.Name + ".servicebus.windows.net"
    clientId = $Poc.ConsumerA2Principal.AppId  # A2 principal
    clientSecret = $Poc.ConsumerA2Principal.Password # A2 password
    tenantId = $Poc.AzureSubscription.TenantId
}

Invoke-WebRequest -Method POST -Uri "$FunctionBaseUrl/api/receive" -Body ($Body | ConvertTo-Json)
```
Expected response: `success: false` and `com.azure.messaging.servicebus.ServiceBusException: Unauthorized access` in the `messages` array.

You can mix and match these subscriptions and principals and see the same thing happen again.
```powershell
# SubscriptionB1 x ConsumerA1
$Body.topicName = $Poc.TopicB.Name
$Body.subscription = $Poc.SubscriptionB1.Name
$Body.clientId = $Poc.ConsumerA1Principal.AppId
$Body.clientSecret = $Poc.ConsumerA1Principal.Password

Invoke-WebRequest -Method POST -Uri "$FunctionBaseUrl/api/receive" -Body ($Body | ConvertTo-Json)
```
```powershell
# SubscriptionA2 x ConsumerB1
$Body.topicName = $Poc.TopicA.Name
$Body.subscription = $Poc.SubscriptionA2.Name
$Body.clientId = $Poc.ConsumerB1Principal.AppId
$Body.clientSecret = $Poc.ConsumerB1Principal.Password

Invoke-WebRequest -Method POST -Uri "$FunctionBaseUrl/api/receive" -Body ($Body | ConvertTo-Json)
```
```powershell
# SubscriptionB1 x ConsumerB2
# Only works with the POC running as an Azure Function
$Body.topicName = $Poc.TopicB.Name
$Body.subscription = $Poc.SubscriptionB1.Name
$Body.clientId = $Poc.ConsumerB2Identity.ClientId
$Body.accountType = "ManagedIdentity"
$Body.remove("clientSecret")

Invoke-WebRequest -Method POST -Uri "$FunctionBaseUrl/api/receive" -Body ($Body | ConvertTo-Json)
```

# Conclusion

The tests conducted by this POC were able to successfuly prove that we can use Azure AD RBAC to secure access to Azure Service Bus Resources both at the Topic and Subscription levels.

# Cleaning up

To cleanup the items created on Azure for this POC (a good practice to save costs), execute the commands bellow:
```powershell
# Delete the principals
az ad sp delete --id $Poc.ControlPrincipal.AppId
az ad sp delete --id $Poc.PublisherPrincipal.AppId
az ad sp delete --id $Poc.ConsumerA1Principal.AppId
az ad sp delete --id $Poc.ConsumerA2Principal.AppId
az ad sp delete --id $Poc.ConsumerB1Principal.AppId
```
```powershell
# Delete the resource group
az group delete --resource-group $Poc.Group.Name
```