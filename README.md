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

Confirm you have the correct subscription:
```powershell
az account show
```

Create the execution group for the POC:
```powershell
$Group = az group create `
    --name "ServiceBusPOC" `
    --location "eastus2" | ConvertFrom-Json
```
> You can use whatever name and location you want because `ConvertFrom-Json` will parse the output of the `az` command and store it in a variable.

Create the Service Bus Namespace:
```powershell
$Namespace = az servicebus namespace create `
    --name "Contracts" `
    --sku standard `
    --location $Group.Location `
    --resource-group $Group.Name | ConvertFrom-Json
```

Create the topics:
```powershell
$TopicA = az servicebus topic create `
    --name "ContractsA" `
    --namespace-name $Namespace.Name `
    --resource-group $Group.Name | ConvertFrom-Json
```
```powershell
$TopicB = az servicebus topic create `
    --name "ContractsB" `
    --namespace-name $Namespace.Name `
    --resource-group $Group.Name | ConvertFrom-Json
```

Create the subscriptions:
```powershell
$SubscriptionA1 = az servicebus topic subscription create `
    --name "ConsumerA1" `
    --topic-name $TopicA.Name `
    --namespace-name $Namespace.Name `
    --resource-group $GroupName `
    --max-delivery-count 1 | ConvertFrom-Json
```
```powershell
$SubscriptionA2 = az servicebus topic subscription create `
    --name "ConsumerA2" `
    --topic-name $TopicA.Name `
    --namespace-name $Namespace.Name `
    --resource-group $GroupName `
    --max-delivery-count 1 | ConvertFrom-Json
```
```powershell
$SubscriptionB1 = az servicebus topic subscription create `
    --name "ConsumerB1" `
    --topic-name $TopicB.Name `
    --namespace-name $Namespace.Name `
    --resource-group $GroupName `
    --max-delivery-count 1 | ConvertFrom-Json
```
```powershell
$SubscriptionB2 = az servicebus topic subscription create `
    --name "ConsumerB2" `
    --topic-name $TopicB.Name `
    --namespace-name $Namespace.Name `
    --resource-group $GroupName `
    --max-delivery-count 1 | ConvertFrom-Json
```

Create the app registrations and service principals:
> Because you won't be able to retrieve the passwords again later, the commands below save them in text files inside your home directory.  
Consumer B2 is a managed identity thus it does not have a password.
```powershell
# Publisher
$PublisherApp = az ad app create `
    --display-name "SBUS-RBAC-POC-$env:USERNAME-Contracts-Publisher" `
    --available-to-other-tenants false | ConvertFrom-Json
$PublisherPrincipal = az ad sp create-for-rbac `
    --name $PublisherApp.DisplayName `
    --role Reader `
    --scopes $Group.Id | ConvertFrom-Json
$PublisherPrincipal.Password > "$env:USERPROFILE\sbus-poc-publisher-pwd.txt"
```
```powershell
# Consumer A1
$ConsumerA1App = az ad app create `
    --display-name "SBUS-RBAC-POC-$env:USERNAME-Consumer-A1" `
    --available-to-other-tenants false | ConvertFrom-Json
$ConsumerA1Principal = az ad sp create-for-rbac `
    --name $ConsumerA1App.DisplayName `
    --role Reader `
    --scopes $Group.Id | ConvertFrom-Json
$ConsumerA1Principal.Password > "$env:USERPROFILE\sbus-poc-consumer-a1-pwd.txt"
```
```powershell
# Consumer A2
$ConsumerA2App = az ad app create `
    --display-name "SBUS-RBAC-POC-$env:USERNAME-Consumer-A2" `
    --available-to-other-tenants false | ConvertFrom-Json
$ConsumerA2Principal = az ad sp create-for-rbac `
    --name $ConsumerA2App.DisplayName `
    --role Reader `
    --scopes $Group.Id | ConvertFrom-Json
$ConsumerA2Principal.Password > "$env:USERPROFILE\sbus-poc-consumer-a2-pwd.txt"
```
```powershell
# Consumer B1
$ConsumerB1App = az ad app create `
    --display-name "SBUS-RBAC-POC-$env:USERNAME-Consumer-B1" `
    --available-to-other-tenants false | ConvertFrom-Json
$ConsumerB1Principal = az ad sp create-for-rbac `
    --name $ConsumerB1App.DisplayName `
    --role Reader `
    --scopes $Group.Id | ConvertFrom-Json
$ConsumerB1Principal.Password > "$env:USERPROFILE\sbus-poc-consumer-b1-pwd.txt"
```
```powershell
# Consumer B2
$ConsumerB2Identity = az identity create `
    --name "Consumer-B2" `
    --resource-group $Group.Name | ConvertFrom-Json
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
    --assignee $PublisherPrincipal.AppId `
    --role $DataSenderRole `
    --scope $TopicA.Id
az role assignment create `
    --assignee $PublisherPrincipal.AppId `
    --role $DataSenderRole `
    --scope $TopicB.Id
```
```powershell
# ConsumerA1 can receive from SubscriptionA1
az role assignment create `
    --assignee $ConsumerA1Principal.AppId `
    --role $DataReceiverRole `
    --scope $SubscriptionA1.Id
```
```powershell
# ConsumerA2 can receive from SubscriptionA2
az role assignment create `
    --assignee $ConsumerA2Principal.AppId `
    --role $DataReceiverRole `
    --scope $SubscriptionA2.Id
```
```powershell
# ConsumerB1 can receive from SubscriptionB1
az role assignment create `
    --assignee $ConsumerB1Principal.AppId `
    --role $DataReceiverRole `
    --scope $SubscriptionB1.Id
```
```powershell
# ConsumerB2 can receive from SubscriptionB2
az role assignment create `
    --assignee $ConsumerB2Identity.PrincipalId `
    --role $DataReceiverRole `
    --scope $SubscriptionB2.Id
```

# Running

This program can be executed locally using:

```
mvn quarkus:dev
```

Or it can be deployed to Microsoft Azure as a Function:
```powershell
mvn clean install azure-functions:deploy `
    -DfunctionAppName="the function app name" `
    -DfunctionAppRegion="the target region" `
    -DfunctionResourceGroup="the target resource group" `
    -Dfunction="the function name" `
    -DfunctionSubscription="the Azure subscription id"
```

# Testing

Notes for the examples:
* `{baseUrl}` is the HTTP location of your service, e.g. `http://localhost:8080` if you're running locally or `https://{functionAppName}.azurewebsites.net` if you're running as an Azure Function.  
* To find the tenant id, run the command: `(az account show | ConvertFrom-Json).TenantId`  
* Replace the placeholder values in the json examples with the proper contents.
* For code simplicity, all responses will return HTTP Status `200` regardless of success or error. Therefore, the reponses will always contain a `success` boolean field indicating success (`true`) or failure (`false`) and a `messages` field array with additional information.
* The function also supports HTTP GETs with the request json fields as query parameters (e.g. `{baseUrl}/api/send?clientId=value&clientSecret=value...`), but for readability the examples will focus on HTTP POST calls only.


## Sending messages

The `send` endpoint will send one message to the specified topic with a given content. However, the function will use the [JavaFaker](https://github.com/DiUS/java-faker) library to send messages with random [Chuck Norris Facts](https://en.wikipedia.org/wiki/Chuck_Norris_facts) if the `message` field is missing. Have fun 😄.

To send a message, make an HTTP POST to `{baseUrl}/api/send` with the following contents:
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

## Receiving messages

The `receive` endpoint will read all messages available for a given subscription and the `messages` field in the response will contain their contents, in the order they were received.

To receive messages using Service Principals, make an HTTP POST to `{baseUrl}/api/receive` with the following contents:
```json
{
    "topicName": "ContractsA or ContractsB",
    "subscription": "SubscriptionA1, SubscriptionA2, SubscriptionB1 or SubscriptionB2",
    "clientId": "consumer appId",
    "clientSecret": "consumer password",
    "tenantId": "azure tenant id"
}
```
To receive messages using Managed Identities, make an HTTP POST to `{baseUrl}/api/receive` with the following contents:
```json
{
    "topicName": "ContractsA or ContractsB",
    "subscription": "SubscriptionA1, SubscriptionA2, SubscriptionB1 or SubscriptionB2",
    "accountType": "ManagedIdentity",
    "clientId": "publisher appId"
}
```